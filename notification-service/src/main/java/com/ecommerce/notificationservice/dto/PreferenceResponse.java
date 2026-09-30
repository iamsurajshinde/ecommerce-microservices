package com.ecommerce.notificationservice.dto;

import com.ecommerce.notificationservice.model.NotificationPreference;

public record PreferenceResponse(
        Long userId, boolean emailEnabled, boolean smsEnabled, boolean pushEnabled) {

    public static PreferenceResponse from(NotificationPreference p) {
        return new PreferenceResponse(
                p.getUserId(), p.isEmailEnabled(), p.isSmsEnabled(), p.isPushEnabled());
    }
}
