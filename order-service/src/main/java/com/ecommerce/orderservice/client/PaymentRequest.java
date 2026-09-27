package com.ecommerce.orderservice.client;

public record PaymentRequest(Long orderId, Double amount, String paymentMethod) {
}
