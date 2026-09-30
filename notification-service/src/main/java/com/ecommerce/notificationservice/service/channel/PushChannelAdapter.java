package com.ecommerce.notificationservice.service.channel;

import com.ecommerce.notificationservice.model.NotificationChannel;
import com.ecommerce.notificationservice.service.RenderedNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Push adapter placeholder. Disabled by default; a real implementation would integrate
 * a provider such as Firebase Cloud Messaging here. Kept as a logging stub so the
 * channel abstraction and dispatch flow are complete and testable.
 */
@Component
public class PushChannelAdapter implements NotificationChannelAdapter {

    private static final Logger log = LoggerFactory.getLogger(PushChannelAdapter.class);

    private final boolean enabled;

    public PushChannelAdapter(
            @Value("${notification.channels.push-enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void send(String recipient, RenderedNotification content) {
        // Integrate a push provider (e.g. Firebase Cloud Messaging) here.
        log.info("[PUSH stub] to={} title='{}'", recipient, content.subject());
    }
}
