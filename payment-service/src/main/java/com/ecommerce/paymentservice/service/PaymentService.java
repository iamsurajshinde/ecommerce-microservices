package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.CheckoutRequest;
import com.ecommerce.paymentservice.dto.CheckoutResponse;
import com.ecommerce.paymentservice.event.EventPublisher;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.service.gateway.GatewayResult;
import com.ecommerce.paymentservice.service.gateway.PaymentGatewaySelector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Set<String> SUPPORTED_METHODS =
            Set.of("CREDIT_CARD", "DEBIT_CARD", "UPI", "NET_BANKING", "WALLET");

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;
    private final PaymentGatewaySelector gatewaySelector;

    public Payment process(Payment payment) {
        if (payment.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID is required.");
        }
        if (payment.getAmount() == null || !Double.isFinite(payment.getAmount())
                || payment.getAmount() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        String method = payment.getPaymentMethod() == null
                ? ""
                : payment.getPaymentMethod().trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_METHODS.contains(method)) {
            throw new IllegalArgumentException("Unsupported payment method: " + payment.getPaymentMethod());
        }
        Payment existingPayment = paymentRepository.findByOrderId(payment.getOrderId()).orElse(null);
        if (existingPayment != null) {
            if (!existingPayment.getAmount().equals(payment.getAmount())
                    || !existingPayment.getPaymentMethod().equals(method)) {
                throw new IllegalStateException("A different payment already exists for this order.");
            }
            return existingPayment;
        }

        payment.setPaymentMethod(method);
        GatewayResult result = gatewaySelector.select().charge(payment);
        payment.setStatus(result.status());
        payment.setTransactionId(result.transactionId());
        payment.setCreatedAt(Instant.now());
        Payment savedPayment = paymentRepository.save(payment);
        publishPaymentSucceeded(savedPayment);
        return savedPayment;
    }

    public CheckoutResponse initiateCheckout(CheckoutRequest request) {
        if (request.orderId() == null) {
            throw new IllegalArgumentException("Order ID is required.");
        }
        if (request.amount() == null || !Double.isFinite(request.amount()) || request.amount() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        String method = request.method() == null
                ? ""
                : request.method().trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_METHODS.contains(method)) {
            throw new IllegalArgumentException("Unsupported payment method: " + request.method());
        }

        Payment existingPayment = paymentRepository.findByOrderId(request.orderId()).orElse(null);
        if (existingPayment != null) {
            if (!existingPayment.getAmount().equals(request.amount())
                    || !existingPayment.getPaymentMethod().equals(method)) {
                throw new IllegalStateException("A different payment already exists for this order.");
            }
            if (!"PENDING".equals(existingPayment.getStatus())) {
                throw new IllegalStateException("Payment already finalized for this order.");
            }
            return new CheckoutResponse(existingPayment.getOrderId(), existingPayment.getStatus(),
                    existingPayment.getAmount(), existingPayment.getPaymentLinkUrl());
        }

        Payment payment = Payment.builder()
                .orderId(request.orderId())
                .amount(request.amount())
                .paymentMethod(method)
                .status("PENDING")
                .createdAt(Instant.now())
                .build();

        String paymentLinkUrl;
        try {
            paymentLinkUrl = gatewaySelector.select().createPaymentLink(payment);
        } catch (UnsupportedOperationException exception) {
            throw new IllegalStateException("Active gateway does not support asynchronous checkout.");
        }

        payment.setPaymentLinkUrl(paymentLinkUrl);
        Payment savedPayment = paymentRepository.save(payment);
        return new CheckoutResponse(savedPayment.getOrderId(), savedPayment.getStatus(),
                savedPayment.getAmount(), savedPayment.getPaymentLinkUrl());
    }

    private void publishPaymentSucceeded(Payment payment) {
        // The Payment entity owns orderId/amount/transactionId but not the user's contact
        // details, so this event carries payment facts only. The user-facing receipt is
        // delivered via the order-service order.confirmed event, which has the email.
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", "payment-succeeded-" + payment.getOrderId());
        payload.put("orderId", payment.getOrderId());
        payload.put("amount", payment.getAmount());
        payload.put("transactionId", payment.getTransactionId());
        eventPublisher.publish(EventPublisher.RK_PAYMENT_SUCCEEDED, payload);
    }

    public Optional<Payment> findByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public Payment refund(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID is required.");
        }
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + orderId));
        if ("REFUNDED".equals(payment.getStatus())) {
            return payment;
        }
        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new IllegalStateException("Only successful payments can be refunded.");
        }
        payment.setStatus("REFUNDED");
        return paymentRepository.save(payment);
    }
}
