package com.ecommerce.orderservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publishes order-lifecycle domain events to the shared {@code ecommerce.events} topic
 * exchange. The {@code __TypeId__} header carries the routing key so the notification
 * service can map the JSON payload to the correct event type. Publishing is best-effort
 * and never breaks the order saga.
 */
@Component
public class EventPublisher {

    public static final String EVENTS_EXCHANGE = "ecommerce.events";
    public static final String RK_ORDER_CONFIRMED = "order.confirmed";
    public static final String RK_ORDER_CANCELLED = "order.cancelled";
    public static final String RK_PAYMENT_FAILED = "payment.failed";

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(String routingKey, Map<String, Object> payload) {
        try {
            rabbitTemplate.convertAndSend(EVENTS_EXCHANGE, routingKey, payload, message -> {
                MessageProperties props = message.getMessageProperties();
                props.setHeader("__TypeId__", routingKey);
                props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                return message;
            });
            log.debug("Published event routingKey={} payload={}", routingKey, payload);
        } catch (AmqpException exception) {
            log.warn("Failed to publish event routingKey={}: {}", routingKey, exception.getMessage());
        }
    }

    @Configuration
    static class RabbitProducerConfig {
        @Bean
        MessageConverter jsonMessageConverter() {
            return new Jackson2JsonMessageConverter();
        }

        @Bean
        RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
            RabbitTemplate template = new RabbitTemplate(connectionFactory);
            template.setMessageConverter(converter);
            return template;
        }
    }
}
