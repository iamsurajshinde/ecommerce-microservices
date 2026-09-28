package com.ecommerce.notificationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Delivery record / audit log for a single notification on a single channel.
 * The (eventId, channel) pair is unique to guarantee idempotent delivery even if
 * an event is redelivered by the broker.
 */
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventId;

    private Long userId;

    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationStatus status;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant sentAt;
}
