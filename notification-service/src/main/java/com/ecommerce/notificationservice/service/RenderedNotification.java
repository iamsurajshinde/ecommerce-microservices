package com.ecommerce.notificationservice.service;

/** Channel-agnostic rendered content ready to be dispatched. */
public record RenderedNotification(String subject, String body) {
}
