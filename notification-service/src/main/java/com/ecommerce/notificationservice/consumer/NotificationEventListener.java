package com.ecommerce.notificationservice.consumer;

import com.ecommerce.notificationservice.config.RabbitConfig;
import com.ecommerce.notificationservice.event.*;
import com.ecommerce.notificationservice.model.NotificationType;
import com.ecommerce.notificationservice.service.NotificationRequest;
import com.ecommerce.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumes domain events from the {@code notifications.q} queue and translates each into
 * a {@link NotificationRequest}. Jackson deserializes to the concrete event type via the
 * type-per-handler {@link RabbitHandler} methods. Any exception propagates so RabbitMQ
 * applies its retry policy and finally dead-letters the message.
 */
@Component
@RabbitListener(queues = RabbitConfig.NOTIFICATIONS_QUEUE)
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitHandler
    public void on(UserRegisteredEvent event) {
        Map<String, Object> model = new HashMap<>();
        model.put("name", event.getName());
        model.put("email", event.getEmail());
        notificationService.handle(new NotificationRequest(
                event.getEventId(), NotificationType.WELCOME,
                event.getUserId(), event.getEmail(), model, false));
    }

    @RabbitHandler
    public void on(OrderConfirmedEvent event) {
        Map<String, Object> model = new HashMap<>();
        model.put("orderId", event.getOrderId());
        model.put("totalPrice", event.getTotalPrice());
        notificationService.handle(new NotificationRequest(
                event.getEventId(), NotificationType.ORDER_CONFIRMED,
                event.getUserId(), event.getEmail(), model, false));
    }

    @RabbitHandler
    public void on(OrderCancelledEvent event) {
        Map<String, Object> model = new HashMap<>();
        model.put("orderId", event.getOrderId());
        model.put("refundIssued", event.getRefundIssued());
        notificationService.handle(new NotificationRequest(
                event.getEventId(), NotificationType.ORDER_CANCELLED,
                event.getUserId(), event.getEmail(), model, false));
    }

    @RabbitHandler
    public void on(PaymentSucceededEvent event) {
        Map<String, Object> model = new HashMap<>();
        model.put("orderId", event.getOrderId());
        model.put("amount", event.getAmount());
        model.put("transactionId", event.getTransactionId());
        notificationService.handle(new NotificationRequest(
                event.getEventId(), NotificationType.PAYMENT_SUCCEEDED,
                event.getUserId(), event.getEmail(), model, false));
    }

    @RabbitHandler
    public void on(PaymentFailedEvent event) {
        Map<String, Object> model = new HashMap<>();
        model.put("orderId", event.getOrderId());
        model.put("amount", event.getAmount());
        model.put("reason", event.getReason());
        notificationService.handle(new NotificationRequest(
                event.getEventId(), NotificationType.PAYMENT_FAILED,
                event.getUserId(), event.getEmail(), model, false));
    }

    @RabbitHandler
    public void on(LowStockEvent event) {
        Map<String, Object> model = new HashMap<>();
        model.put("productId", event.getProductId());
        model.put("productName", event.getProductName());
        model.put("remainingStock", event.getRemainingStock());
        // Operational alert: no end user, dispatched to the ops recipient.
        notificationService.handle(new NotificationRequest(
                event.getEventId(), NotificationType.LOW_STOCK_ALERT,
                null, null, model, true));
    }

    @RabbitHandler(isDefault = true)
    public void onUnknown(Object message) {
        log.warn("Received unsupported event payload: {}", message);
    }
}
