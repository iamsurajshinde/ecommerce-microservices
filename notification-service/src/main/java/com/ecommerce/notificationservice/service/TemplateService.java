package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.model.NotificationType;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * Renders channel-agnostic subject/body per notification type. Uses simple, dependency-free
 * string templating over the event model. For richer HTML email a Thymeleaf template could
 * be substituted here without changing callers.
 */
@Service
public class TemplateService {

    public RenderedNotification render(NotificationType type, Map<String, Object> model) {
        Map<String, Object> m = model == null ? Map.of() : model;
        return switch (type) {
            case WELCOME -> new RenderedNotification(
                    "Welcome to our store, " + str(m, "name", "there") + "!",
                    "Hi " + str(m, "name", "there") + ",\n\n"
                            + "Your account (" + str(m, "email", "") + ") has been created successfully. "
                            + "Happy shopping!");
            case ORDER_CONFIRMED -> new RenderedNotification(
                    "Order #" + str(m, "orderId", "") + " confirmed",
                    "Your order #" + str(m, "orderId", "") + " has been confirmed.\n"
                            + "Total: " + str(m, "totalPrice", "0.0") + "\n\n"
                            + "Thank you for shopping with us.");
            case ORDER_CANCELLED -> new RenderedNotification(
                    "Order #" + str(m, "orderId", "") + " cancelled",
                    "Your order #" + str(m, "orderId", "") + " has been cancelled.\n"
                            + (bool(m, "refundIssued")
                                ? "A refund has been issued to your original payment method."
                                : "No payment had been captured, so no refund is required."));
            case PAYMENT_SUCCEEDED -> new RenderedNotification(
                    "Payment received for order #" + str(m, "orderId", ""),
                    "We have received your payment of " + str(m, "amount", "0.0")
                            + " for order #" + str(m, "orderId", "") + ".\n"
                            + "Transaction ID: " + str(m, "transactionId", "N/A"));
            case PAYMENT_FAILED -> new RenderedNotification(
                    "Payment failed for order #" + str(m, "orderId", ""),
                    "Unfortunately your payment of " + str(m, "amount", "0.0")
                            + " for order #" + str(m, "orderId", "") + " could not be processed.\n"
                            + "Reason: " + str(m, "reason", "unknown") + "\n\n"
                            + "Please try again or use a different payment method.");
            case LOW_STOCK_ALERT -> new RenderedNotification(
                    "Low stock alert: " + str(m, "productName", "product"),
                    "Product '" + str(m, "productName", "")
                            + "' (ID " + str(m, "productId", "") + ") is low on stock. "
                            + "Remaining: " + str(m, "remainingStock", "0") + ". Consider restocking.");
        };
    }

    private String str(Map<String, Object> model, String key, String fallback) {
        Object value = model.get(key);
        return value == null ? fallback : Objects.toString(value);
    }

    private boolean bool(Map<String, Object> model, String key) {
        Object value = model.get(key);
        return value instanceof Boolean b ? b : Boolean.parseBoolean(Objects.toString(value, "false"));
    }
}
