package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.model.Notification;
import com.ecommerce.notificationservice.model.NotificationChannel;
import com.ecommerce.notificationservice.model.NotificationStatus;
import com.ecommerce.notificationservice.repository.NotificationRepository;
import com.ecommerce.notificationservice.service.channel.NotificationChannelAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Core orchestration: renders content, resolves target channels, delivers through the
 * matching adapters, and records an audit/idempotency row per (event, channel).
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final PreferenceService preferenceService;
    private final TemplateService templateService;
    private final Map<NotificationChannel, NotificationChannelAdapter> adapters =
            new EnumMap<>(NotificationChannel.class);
    private final String opsAlertRecipient;

    public NotificationService(
            NotificationRepository notificationRepository,
            PreferenceService preferenceService,
            TemplateService templateService,
            List<NotificationChannelAdapter> channelAdapters,
            @Value("${notification.ops-alert-recipient}") String opsAlertRecipient) {
        this.notificationRepository = notificationRepository;
        this.preferenceService = preferenceService;
        this.templateService = templateService;
        this.opsAlertRecipient = opsAlertRecipient;
        for (NotificationChannelAdapter adapter : channelAdapters) {
            this.adapters.put(adapter.channel(), adapter);
        }
    }

    /**
     * Process one notification request. Delivers on every eligible channel; each channel
     * is idempotent and independently recorded, so a broker redelivery will not resend an
     * already-sent channel.
     */
    public void handle(NotificationRequest request) {
        RenderedNotification content = templateService.render(request.type(), request.model());

        for (NotificationChannel channel : resolveChannels(request)) {
            deliverOnChannel(request, channel, content);
        }
    }

    private List<NotificationChannel> resolveChannels(NotificationRequest request) {
        // Operational alerts (e.g. low stock) always go to the ops email, bypassing user prefs.
        if (request.opsAlert()) {
            return List.of(NotificationChannel.EMAIL);
        }
        return preferenceService.enabledChannels(request.userId());
    }

    private void deliverOnChannel(
            NotificationRequest request, NotificationChannel channel, RenderedNotification content) {
        // Idempotency guard: skip if this event was already handled on this channel.
        if (notificationRepository.existsByEventIdAndChannel(request.eventId(), channel)) {
            log.debug("Skipping duplicate notification event={} channel={}", request.eventId(), channel);
            return;
        }

        String recipient = request.opsAlert() ? opsAlertRecipient : request.recipient();

        Notification record = Notification.builder()
                .eventId(request.eventId())
                .userId(request.userId())
                .recipient(recipient)
                .channel(channel)
                .type(request.type())
                .status(NotificationStatus.PENDING)
                .subject(content.subject())
                .body(content.body())
                .attempts(0)
                .createdAt(Instant.now())
                .build();

        NotificationChannelAdapter adapter = adapters.get(channel);
        if (adapter == null || !adapter.isEnabled()) {
            record.setStatus(NotificationStatus.SKIPPED);
            record.setFailureReason("Channel " + channel + " is not enabled.");
            notificationRepository.save(record);
            log.debug("Channel {} disabled; recorded SKIPPED for event={}", channel, request.eventId());
            return;
        }

        record.setAttempts(1);
        try {
            adapter.send(recipient, content);
            record.setStatus(NotificationStatus.SENT);
            record.setSentAt(Instant.now());
            notificationRepository.save(record);
            log.info("Notification SENT event={} type={} channel={}",
                    request.eventId(), request.type(), channel);
        } catch (RuntimeException exception) {
            record.setStatus(NotificationStatus.FAILED);
            record.setFailureReason(truncate(exception.getMessage()));
            notificationRepository.save(record);
            log.warn("Notification FAILED event={} channel={}: {}",
                    request.eventId(), channel, exception.getMessage());
            // Re-throw so the RabbitMQ listener retries / dead-letters the message.
            throw exception;
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "Unknown delivery error.";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
