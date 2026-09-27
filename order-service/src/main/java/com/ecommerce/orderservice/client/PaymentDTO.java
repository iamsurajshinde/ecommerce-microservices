package com.ecommerce.orderservice.client;

public record PaymentDTO(Long id, Long orderId, Double amount, String paymentMethod,
                         String status, String transactionId, String failureReason) {
}
