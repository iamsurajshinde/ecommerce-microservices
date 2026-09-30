package com.ecommerce.notificationservice.service.channel;

import com.ecommerce.notificationservice.model.NotificationChannel;
import com.ecommerce.notificationservice.service.RenderedNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SMS adapter placeholder. Disabled by default; a real implementation would integrate
 * a provider such as Twilio here. Kept as a logging stub so the channel abstraction and
 * dispatch flow are complete and testable without external credentials.
 */
@Component
public class SmsChannelAdapter implements NotificationChannelAdapter {

    private static final Logger log = LoggerFactory.getLogger(SmsChannelAdapter.class);

    private final boolean enabled;

    public SmsChannelAdapter(
            @Value("${notification.channels.sms-enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void send(String recipient, RenderedNotification content) {
        // Integrate an SMS provider (e.g. Twilio) here.
        log.info("[SMS stub] to={} body='{}'", recipient, content.body());
    }
}
