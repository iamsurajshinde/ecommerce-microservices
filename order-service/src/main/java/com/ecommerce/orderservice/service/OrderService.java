package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.CheckoutRequest;
import com.ecommerce.orderservice.client.CheckoutResponse;
import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.client.ProductDTO;
import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.PaymentDTO;
import com.ecommerce.orderservice.client.PaymentRequest;
import com.ecommerce.orderservice.client.UserClient;
import com.ecommerce.orderservice.client.UserDTO;
import com.ecommerce.orderservice.event.EventPublisher;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final UserClient userClient;
    private final PaymentClient paymentClient;
    private final EventPublisher eventPublisher;

    @Value("${order.payment.async:false}")
    private boolean async;

    public Order placeOrder(Order order) {
        UserDTO user = order.getUserId() == null ? null : userClient.getUserById(order.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("User not found.");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one order item is required.");
        }
        double totalCalculatedPrice = 0.0;

        for (OrderItem item : order.getItems()) {
            if (item == null || item.getProductId() == null
                    || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Each order item requires a product and positive quantity.");
            }
            ProductDTO product = productClient.getProductById(item.getProductId());

            if (product == null) {
                throw new RuntimeException("Product with ID " + item.getProductId() + " not found.");
            }
            if (product.stockQuantity() == null
                    || product.stockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.name());
            }
            if (product.price() == null || !Double.isFinite(product.price())) {
                throw new RuntimeException("Product has an invalid price: " + product.name());
            }

            item.setPrice(product.price());
            totalCalculatedPrice += product.price() * item.getQuantity();
        }

        order.setTotalPrice(totalCalculatedPrice);
        order.setStatus(OrderStatus.CREATED);
        order.setPaymentStatus("PENDING");
        Order savedOrder = orderRepository.save(order);
        List<OrderItem> reservedItems = new ArrayList<>();

        if (!async) {
            try {
                for (OrderItem item : savedOrder.getItems()) {
                    productClient.decreaseStock(item.getProductId(), item.getQuantity());
                    reservedItems.add(item);
                }
                PaymentDTO payment = paymentClient.processPayment(new PaymentRequest(
                        savedOrder.getId(), savedOrder.getTotalPrice(), savedOrder.getPaymentMethod()));
                if (payment == null || !"SUCCESS".equals(payment.status())) {
                    throw new IllegalStateException("Payment was not successful.");
                }
                savedOrder.setPaymentStatus("SUCCESS");
                savedOrder.setStatus(OrderStatus.CONFIRMED);
            } catch (FeignException | IllegalArgumentException | IllegalStateException exception) {
                RuntimeException compensationFailure = restoreReservedStock(reservedItems);
                savedOrder.setPaymentStatus("FAILED");
                savedOrder.setStatus(OrderStatus.PAYMENT_FAILED);
                Order failedOrder = orderRepository.save(savedOrder);
                publishPaymentFailed(failedOrder, user, exception.getMessage());
                if (compensationFailure != null) {
                    exception.addSuppressed(compensationFailure);
                }
                throw new IllegalStateException("Payment processing failed.", exception);
            }
            Order confirmedOrder = orderRepository.save(savedOrder);
            publishOrderConfirmed(confirmedOrder, user);
            return confirmedOrder;
        } else {
            try {
                for (OrderItem item : savedOrder.getItems()) {
                    productClient.decreaseStock(item.getProductId(), item.getQuantity());
                    reservedItems.add(item);
                }
                CheckoutResponse resp = paymentClient.checkout(new CheckoutRequest(
                        savedOrder.getId(), savedOrder.getTotalPrice(), savedOrder.getPaymentMethod(), null));
                savedOrder.setPaymentLinkUrl(resp == null ? null : resp.paymentLinkUrl());
                // Leave status=CREATED, paymentStatus="PENDING"; confirmation arrives via webhook event.
                Order pendingOrder = orderRepository.save(savedOrder);
                return pendingOrder;
            } catch (FeignException exception) {
                RuntimeException compensationFailure = restoreReservedStock(reservedItems);
                savedOrder.setPaymentStatus("FAILED");
                savedOrder.setStatus(OrderStatus.PAYMENT_FAILED);
                Order failedOrder = orderRepository.save(savedOrder);
                publishPaymentFailed(failedOrder, user, exception.getMessage());
                if (compensationFailure != null) {
                    exception.addSuppressed(compensationFailure);
                }
                throw new IllegalStateException("Payment checkout failed.", exception);
            }
        }
    }

    /**
     * Idempotent async confirmation driven by a {@code payment.succeeded} event. Acts only
     * when the order is still awaiting payment (CREATED/PENDING); repeat deliveries for an
     * already-terminal order are a silent no-op so Stripe/broker retries never double-confirm.
     * Runs OUTSIDE an HTTP request, so {@code userClient.getUserById} forwards only the
     * internal service token and may return null; {@link #publishOrderConfirmed} null-guards it.
     */
    public void confirmFromPayment(Long orderId) {
        if (orderId == null) {
            return;
        }
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        if (order.getStatus() != OrderStatus.CREATED || !"PENDING".equals(order.getPaymentStatus())) {
            return;
        }
        order.setPaymentStatus("SUCCESS");
        order.setStatus(OrderStatus.CONFIRMED);
        Order confirmedOrder = orderRepository.save(order);
        UserDTO user = confirmedOrder.getUserId() == null
                ? null : userClient.getUserById(confirmedOrder.getUserId());
        publishOrderConfirmed(confirmedOrder, user);
    }

    /**
     * Idempotent async failure driven by a {@code payment.failed} event. Acts only when the
     * order is still awaiting payment (CREATED/PENDING): restores reserved stock, marks the
     * order PAYMENT_FAILED, and publishes {@code order.cancelled} (refundIssued=false). Repeat
     * deliveries for a terminal order are a silent no-op. Runs OUTSIDE an HTTP request.
     */
    public void failFromPayment(Long orderId, String reason) {
        if (orderId == null) {
            return;
        }
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        if (order.getStatus() != OrderStatus.CREATED || !"PENDING".equals(order.getPaymentStatus())) {
            return;
        }
        restoreReservedStock(order.getItems());
        order.setPaymentStatus("FAILED");
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        Order failedOrder = orderRepository.save(order);
        publishOrderCancelled(failedOrder, false);
    }

    private void publishOrderConfirmed(Order order, UserDTO user) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", "order-confirmed-" + order.getId());
        payload.put("orderId", order.getId());
        payload.put("userId", order.getUserId());
        payload.put("email", user == null ? null : user.email());
        payload.put("totalPrice", order.getTotalPrice());
        eventPublisher.publish(EventPublisher.RK_ORDER_CONFIRMED, payload);
    }

    private void publishPaymentFailed(Order order, UserDTO user, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", "payment-failed-" + order.getId());
        payload.put("orderId", order.getId());
        payload.put("userId", order.getUserId());
        payload.put("email", user == null ? null : user.email());
        payload.put("amount", order.getTotalPrice());
        payload.put("reason", reason == null ? "Payment processing failed." : reason);
        eventPublisher.publish(EventPublisher.RK_PAYMENT_FAILED, payload);
    }

    private RuntimeException restoreReservedStock(List<OrderItem> reservedItems) {
        RuntimeException firstFailure = null;
        for (OrderItem item : reservedItems) {
            try {
                productClient.restoreStock(item.getProductId(), item.getQuantity());
            } catch (FeignException | IllegalArgumentException | IllegalStateException exception) {
                if (firstFailure == null) {
                    firstFailure = new IllegalStateException("Unable to restore reserved stock.", exception);
                } else {
                    firstFailure.addSuppressed(exception);
                }
            }
        }
        return firstFailure;
    }

    public Page<Order> getOrdersByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));
    }

    public Order cancelOrder(Long id) {
        Order order = getOrder(id);
        if (OrderStatus.CANCELLED == order.getStatus()) {
            return order;
        }

        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order cannot be cancelled in its current state.");
        }
        for (OrderItem item : order.getItems()) {
            productClient.restoreStock(item.getProductId(), item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
        boolean refundIssued = false;
        if ("SUCCESS".equals(order.getPaymentStatus())) {
            paymentClient.refundPayment(order.getId());
            order.setPaymentStatus("REFUNDED");
            refundIssued = true;
        }
        Order cancelledOrder = orderRepository.save(order);
        publishOrderCancelled(cancelledOrder, refundIssued);
        return cancelledOrder;
    }

    private void publishOrderCancelled(Order order, boolean refundIssued) {
        UserDTO user = order.getUserId() == null ? null : userClient.getUserById(order.getUserId());
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", "order-cancelled-" + order.getId());
        payload.put("orderId", order.getId());
        payload.put("userId", order.getUserId());
        payload.put("email", user == null ? null : user.email());
        payload.put("refundIssued", refundIssued);
        eventPublisher.publish(EventPublisher.RK_ORDER_CANCELLED, payload);
    }

    public Order updateStatus(Long id, String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Order status is required.");
        }
        OrderStatus normalized;
        try {
            normalized = OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported order status.");
        }
        Order order = getOrder(id);
        order.setStatus(normalized);
        return orderRepository.save(order);
    }
}