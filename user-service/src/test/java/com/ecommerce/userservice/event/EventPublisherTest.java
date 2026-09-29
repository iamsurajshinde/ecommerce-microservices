package com.ecommerce.userservice.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link EventPublisher#publish(String, Map)}.
 *
 * <p>These verify the publish contract without a running broker: the correct exchange,
 * routing key, and payload are sent; the {@code __TypeId__} header and JSON content type
 * are applied by the message post-processor; and a broker failure is swallowed
 * (best-effort publishing).
 */
@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventPublisher eventPublisher;

    @Test
    void publishSendsToEventsExchangeWithRoutingKeyAndPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", "user-registered-1-abc");
        payload.put("userId", 1L);
        payload.put("email", "ada@example.com");
        payload.put("name", "Ada");

        eventPublisher.publish(EventPublisher.RK_USER_REGISTERED, payload);

        verify(rabbitTemplate).convertAndSend(
                eq(EventPublisher.EVENTS_EXCHANGE),
                eq(EventPublisher.RK_USER_REGISTERED),
                eq((Object) payload),
                any(MessagePostProcessor.class));
    }

    @Test
    void publishSetsTypeIdHeaderAndJsonContentType() throws Exception {
        Map<String, Object> payload = Map.of("eventId", "user-registered-1-abc");

        eventPublisher.publish(EventPublisher.RK_USER_REGISTERED, payload);

        // Capture the post-processor and run it against a real Message to assert headers.
        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(EventPublisher.EVENTS_EXCHANGE),
                eq(EventPublisher.RK_USER_REGISTERED),
                eq((Object) payload),
                captor.capture());

        Message message = new Message(new byte[0], new MessageProperties());
        Message processed = captor.getValue().postProcessMessage(message);

        MessageProperties props = processed.getMessageProperties();
        assertThat(props.getHeader("__TypeId__")).isEqualTo(EventPublisher.RK_USER_REGISTERED);
        assertThat(props.getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
    }

    @Test
    void publishSwallowsBrokerFailure() {
        doThrow(new AmqpException("broker down"))
                .when(rabbitTemplate).convertAndSend(
                        eq(EventPublisher.EVENTS_EXCHANGE),
                        eq(EventPublisher.RK_USER_REGISTERED),
                        any(Object.class),
                        any(MessagePostProcessor.class));

        // Best-effort publishing: the exception must not propagate to the caller.
        assertThatCode(() ->
                eventPublisher.publish(EventPublisher.RK_USER_REGISTERED, Map.of("eventId", "x")))
                .doesNotThrowAnyException();
    }
}
