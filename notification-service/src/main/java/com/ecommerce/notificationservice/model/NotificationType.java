package com.ecommerce.notificationservice.model;

/**
 * The business scenarios that produce a notification. Each maps to a routing key
 * published by an upstream service (see RabbitConfig).
 */
public enum NotificationType {
    WELCOME,              // user.registered
    ORDER_CONFIRMED,      // order.confirmed
    ORDER_CANCELLED,      // order.cancelled
    PAYMENT_SUCCEEDED,    // payment.succeeded
    PAYMENT_FAILED,       // payment.failed
    LOW_STOCK_ALERT       // product.low_stock (operational alert)
}
