package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.model.NotificationChannel;
import com.ecommerce.notificationservice.model.NotificationPreference;
import com.ecommerce.notificationservice.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves which channels are permitted for a user, combining the user's stored
 * preferences (or platform defaults when none exist) with the per-user record.
 */
@Service
public class PreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    public PreferenceService(NotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    /** Default preferences applied when a user has no stored row. */
    public NotificationPreference defaults(Long userId) {
        return NotificationPreference.builder()
                .userId(userId)
                .emailEnabled(true)
                .smsEnabled(false)
                .pushEnabled(false)
                .build();
    }

    public NotificationPreference getOrDefault(Long userId) {
        return preferenceRepository.findByUserId(userId).orElseGet(() -> defaults(userId));
    }

    public NotificationPreference save(Long userId, boolean email, boolean sms, boolean push) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> defaults(userId));
        preference.setEmailEnabled(email);
        preference.setSmsEnabled(sms);
        preference.setPushEnabled(push);
        return preferenceRepository.save(preference);
    }

    /** Channels the user has opted into. */
    public List<NotificationChannel> enabledChannels(Long userId) {
        NotificationPreference preference = getOrDefault(userId);
        List<NotificationChannel> channels = new ArrayList<>();
        if (preference.isEmailEnabled()) {
            channels.add(NotificationChannel.EMAIL);
        }
        if (preference.isSmsEnabled()) {
            channels.add(NotificationChannel.SMS);
        }
        if (preference.isPushEnabled()) {
            channels.add(NotificationChannel.PUSH);
        }
        return channels;
    }
}
