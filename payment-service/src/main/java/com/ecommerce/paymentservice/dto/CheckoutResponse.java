package com.ecommerce.paymentservice.dto;

public record CheckoutResponse(Long orderId, String status, Double amount, String paymentLinkUrl) {}
