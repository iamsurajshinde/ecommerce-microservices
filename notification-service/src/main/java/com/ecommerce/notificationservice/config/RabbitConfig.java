package com.ecommerce.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ecommerce.notificationservice.event.LowStockEvent;
import com.ecommerce.notificationservice.event.OrderCancelledEvent;
import com.ecommerce.notificationservice.event.OrderConfirmedEvent;
import com.ecommerce.notificationservice.event.PaymentFailedEvent;
import com.ecommerce.notificationservice.event.PaymentSucceededEvent;
import com.ecommerce.notificationservice.event.UserRegisteredEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ topology for the notification service.
 *
 * <p>A single durable topic exchange ({@code ecommerce.events}) receives all domain
 * events. The notification service binds one durable queue ({@code notifications.q})
 * to the routing patterns it cares about. Messages that exhaust their retry attempts
 * are dead-lettered to {@code notifications.dlq} via a dedicated dead-letter exchange.
 */
@Configuration
public class RabbitConfig {

    public static final String EVENTS_EXCHANGE = "ecommerce.events";

    public static final String NOTIFICATIONS_QUEUE = "notifications.q";
    public static final String DEAD_LETTER_EXCHANGE = "notifications.dlx";
    public static final String DEAD_LETTER_QUEUE = "notifications.dlq";

    // Routing keys (also used by producers in other services).
    public static final String RK_ORDER_CONFIRMED = "order.confirmed";
    public static final String RK_ORDER_CANCELLED = "order.cancelled";
    public static final String RK_PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String RK_PAYMENT_FAILED = "payment.failed";
    public static final String RK_USER_REGISTERED = "user.registered";
    public static final String RK_LOW_STOCK = "product.low_stock";

    @Bean
    TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange deadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue notificationsQueue() {
        return QueueBuilder.durable(NOTIFICATIONS_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding orderBinding(Queue notificationsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(eventsExchange).with("order.*");
    }

    @Bean
    Binding paymentBinding(Queue notificationsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(eventsExchange).with("payment.*");
    }

    @Bean
    Binding userBinding(Queue notificationsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(eventsExchange).with("user.*");
    }

    @Bean
    Binding productBinding(Queue notificationsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(eventsExchange).with("product.*");
    }

    @Bean
    Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_QUEUE);
    }

    // Stable, package-independent type ids so producers in other services can publish
    // events without sharing this module's class names. Producers set a "__TypeId__"
    // header equal to the keys below (e.g. "order.confirmed").
    public static final String TYPE_ID_HEADER = "__TypeId__";

    @Bean
    DefaultClassMapper classMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put(RK_USER_REGISTERED, UserRegisteredEvent.class);
        idClassMapping.put(RK_ORDER_CONFIRMED, OrderConfirmedEvent.class);
        idClassMapping.put(RK_ORDER_CANCELLED, OrderCancelledEvent.class);
        idClassMapping.put(RK_PAYMENT_SUCCEEDED, PaymentSucceededEvent.class);
        idClassMapping.put(RK_PAYMENT_FAILED, PaymentFailedEvent.class);
        idClassMapping.put(RK_LOW_STOCK, LowStockEvent.class);
        classMapper.setIdClassMapping(idClassMapping);
        return classMapper;
    }

    @Bean
    MessageConverter jsonMessageConverter(DefaultClassMapper classMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setClassMapper(classMapper);
        return converter;
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setExchange(EVENTS_EXCHANGE);
        return template;
    }
}
