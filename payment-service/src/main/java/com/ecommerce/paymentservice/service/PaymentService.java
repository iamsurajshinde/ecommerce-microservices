package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Set<String> SUPPORTED_METHODS =
            Set.of("CREDIT_CARD", "DEBIT_CARD", "UPI", "NET_BANKING", "WALLET");

    private final PaymentRepository paymentRepository;

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
        payment.setStatus("SUCCESS");
        payment.setTransactionId("MOCK-" + UUID.randomUUID());
        payment.setCreatedAt(Instant.now());
        return paymentRepository.save(payment);
    }

    public Optional<Payment> findByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}
