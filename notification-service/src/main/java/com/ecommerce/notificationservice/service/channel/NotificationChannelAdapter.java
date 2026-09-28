package com.ecommerce.notificationservice.service.channel;

import com.ecommerce.notificationservice.model.NotificationChannel;
import com.ecommerce.notificationservice.service.RenderedNotification;

/**
 * A delivery channel (email, SMS, push). Implementations encapsulate the provider
 * integration and throw an exception on delivery failure so the caller can record it
 * and let RabbitMQ retry / dead-letter the message.
 */
public interface NotificationChannelAdapter {

    NotificationChannel channel();

    /** True when this channel is globally enabled by configuration. */
    boolean isEnabled();

    /**
     * Deliver the rendered content to the recipient.
     *
     * @throws RuntimeException if delivery fails
     */
    void send(String recipient, RenderedNotification content);
}
