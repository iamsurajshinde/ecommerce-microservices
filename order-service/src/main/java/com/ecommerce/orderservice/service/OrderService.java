package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.client.ProductDTO;
import com.ecommerce.orderservice.client.UserClient;
import com.ecommerce.orderservice.client.PaymentClient;
import com.ecommerce.orderservice.client.PaymentDTO;
import com.ecommerce.orderservice.client.PaymentRequest;
import feign.FeignException;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final UserClient userClient;
    private final PaymentClient paymentClient;

    public Order placeOrder(Order order) {
        if (order.getUserId() == null || userClient.getUserById(order.getUserId()) == null) {
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
        order.setStatus("CREATED");
        order.setPaymentStatus("PENDING");
        Order savedOrder = orderRepository.save(order);
        List<OrderItem> reservedItems = new java.util.ArrayList<>();

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
            savedOrder.setStatus("CONFIRMED");
        } catch (FeignException | IllegalArgumentException | IllegalStateException exception) {
            RuntimeException compensationFailure = restoreReservedStock(reservedItems);
            savedOrder.setPaymentStatus("FAILED");
            savedOrder.setStatus("PAYMENT_FAILED");
            orderRepository.save(savedOrder);
            if (compensationFailure != null) {
                exception.addSuppressed(compensationFailure);
            }
            throw new IllegalStateException("Payment processing failed.", exception);
        }
        return orderRepository.save(savedOrder);
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

    public List<Order> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }
}