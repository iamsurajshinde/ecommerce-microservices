package com.ecommerce.notificationservice.dto;

public record PreferenceRequest(Boolean emailEnabled, Boolean smsEnabled, Boolean pushEnabled) {
}
