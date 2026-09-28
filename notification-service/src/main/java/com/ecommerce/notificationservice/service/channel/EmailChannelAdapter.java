package com.ecommerce.notificationservice.service.channel;

import com.ecommerce.notificationservice.model.NotificationChannel;
import com.ecommerce.notificationservice.service.RenderedNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailChannelAdapter implements NotificationChannelAdapter {

    private static final Logger log = LoggerFactory.getLogger(EmailChannelAdapter.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean enabled;

    public EmailChannelAdapter(
            JavaMailSender mailSender,
            @Value("${notification.mail.from}") String from,
            @Value("${notification.channels.email-enabled:true}") boolean enabled) {
        this.mailSender = mailSender;
        this.from = from;
        this.enabled = enabled;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void send(String recipient, RenderedNotification content) {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("Email recipient is required.");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(content.subject());
        message.setText(content.body());
        mailSender.send(message);
        log.debug("Email sent to {} with subject '{}'", recipient, content.subject());
    }
}
