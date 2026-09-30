package com.ecommerce.notificationservice.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Per-user channel opt-in settings. Absence of a row means platform defaults apply
 * (email on, sms/push off).
 */
@Entity
@Table(name = "notification_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private boolean emailEnabled;

    @Column(nullable = false)
    private boolean smsEnabled;

    @Column(nullable = false)
    private boolean pushEnabled;
}
