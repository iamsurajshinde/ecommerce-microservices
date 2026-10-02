package com.ecommerce.orderservice.config;

import com.ecommerce.orderservice.event.PaymentFailedEvent;
import com.ecommerce.orderservice.event.PaymentSucceededEvent;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumer-side RabbitMQ topology for the order service.
 *
 * <p>The order service binds its OWN durable queue ({@code order.payments.q}) to the
 * shared {@code ecommerce.events} topic exchange on {@code payment.*} so it receives
 * {@code payment.succeeded} and {@code payment.failed}. A {@link DefaultClassMapper}
 * keyed on the routing-key strings (carried in the {@code __TypeId__} header) resolves
 * the JSON payload to the local typed event classes. This mirrors notification-service's
 * converter wiring without reusing its queue, and leaves the producer config in
 * {@code event/EventPublisher.java} untouched.
 */
@Configuration
public class RabbitConsumerConfig {

    public static final String EVENTS_EXCHANGE = "ecommerce.events";
    public static final String ORDER_PAYMENTS_QUEUE = "order.payments.q";

    public static final String RK_PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String RK_PAYMENT_FAILED = "payment.failed";

    @Bean
    TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    Queue orderPaymentsQueue() {
        return QueueBuilder.durable(ORDER_PAYMENTS_QUEUE).build();
    }

    @Bean
    Binding orderPaymentsBinding(Queue orderPaymentsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(orderPaymentsQueue).to(eventsExchange).with("payment.*");
    }

    @Bean
    DefaultClassMapper paymentEventClassMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put(RK_PAYMENT_SUCCEEDED, PaymentSucceededEvent.class);
        idClassMapping.put(RK_PAYMENT_FAILED, PaymentFailedEvent.class);
        classMapper.setIdClassMapping(idClassMapping);
        return classMapper;
    }

    @Bean
    Jackson2JsonMessageConverter paymentEventMessageConverter(DefaultClassMapper paymentEventClassMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setClassMapper(paymentEventClassMapper);
        return converter;
    }

    @Bean
    SimpleRabbitListenerContainerFactory paymentListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter paymentEventMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(paymentEventMessageConverter);
        return factory;
    }
}
