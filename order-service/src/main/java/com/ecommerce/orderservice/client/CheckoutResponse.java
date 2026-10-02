package com.ecommerce.orderservice.client;

public record CheckoutResponse(Long orderId, String status, Double amount, String paymentLinkUrl) {}
