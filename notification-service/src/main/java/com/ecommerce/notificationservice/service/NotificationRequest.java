package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.model.NotificationType;

import java.util.Map;

/**
 * Normalized, channel-agnostic instruction produced by the event listener from a
 * domain event and handed to the NotificationService for delivery.
 *
 * @param eventId    idempotency key from the source event
 * @param type       business scenario driving the notification
 * @param userId     target user (nullable for operational alerts like low stock)
 * @param recipient  resolved contact (email address, phone, device token)
 * @param model      template variables used to render subject/body
 * @param opsAlert   true when this is an operational alert to the admin, not a user
 */
public record NotificationRequest(
        String eventId,
        NotificationType type,
        Long userId,
        String recipient,
        Map<String, Object> model,
        boolean opsAlert) {
}
