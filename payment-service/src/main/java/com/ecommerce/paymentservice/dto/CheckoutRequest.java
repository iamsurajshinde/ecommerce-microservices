package com.ecommerce.paymentservice.dto;

public record CheckoutRequest(Long orderId, Double amount, String method, String currency) {}
