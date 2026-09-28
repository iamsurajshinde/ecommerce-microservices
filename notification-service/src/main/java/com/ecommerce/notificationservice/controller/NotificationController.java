package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.NotificationResponse;
import com.ecommerce.notificationservice.dto.PreferenceRequest;
import com.ecommerce.notificationservice.dto.PreferenceResponse;
import com.ecommerce.notificationservice.model.NotificationPreference;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import com.ecommerce.notificationservice.service.PreferenceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final PreferenceService preferenceService;

    public NotificationController(
            NotificationRepository notificationRepository,
            PreferenceService preferenceService) {
        this.notificationRepository = notificationRepository;
        this.preferenceService = preferenceService;
    }

    @GetMapping("/user/{userId}")
    public List<NotificationResponse> getUserNotifications(@PathVariable Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @GetMapping("/preferences/{userId}")
    public ResponseEntity<PreferenceResponse> getPreferences(@PathVariable Long userId) {
        return ResponseEntity.ok(PreferenceResponse.from(preferenceService.getOrDefault(userId)));
    }

    @PutMapping("/preferences/{userId}")
    public ResponseEntity<PreferenceResponse> updatePreferences(
            @PathVariable Long userId, @RequestBody PreferenceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Preference request is required.");
        }
        NotificationPreference current = preferenceService.getOrDefault(userId);
        boolean email = request.emailEnabled() != null ? request.emailEnabled() : current.isEmailEnabled();
        boolean sms = request.smsEnabled() != null ? request.smsEnabled() : current.isSmsEnabled();
        boolean push = request.pushEnabled() != null ? request.pushEnabled() : current.isPushEnabled();
        return ResponseEntity.ok(PreferenceResponse.from(
                preferenceService.save(userId, email, sms, push)));
    }
}
