package com.ecommerce.orderservice.client;

public record CheckoutRequest(Long orderId, Double amount, String method, String currency) {}
