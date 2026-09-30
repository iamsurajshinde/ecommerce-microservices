package com.ecommerce.notificationservice.dto;

import com.ecommerce.notificationservice.model.Notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String eventId,
        Long userId,
        String recipient,
        String channel,
        String type,
        String status,
        String subject,
        String failureReason,
        int attempts,
        Instant createdAt,
        Instant sentAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getEventId(), n.getUserId(), n.getRecipient(),
                n.getChannel() == null ? null : n.getChannel().name(),
                n.getType() == null ? null : n.getType().name(),
                n.getStatus() == null ? null : n.getStatus().name(),
                n.getSubject(), n.getFailureReason(), n.getAttempts(),
                n.getCreatedAt(), n.getSentAt());
    }
}
