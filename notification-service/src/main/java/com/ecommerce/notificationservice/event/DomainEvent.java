package com.ecommerce.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Base contract for all incoming domain events consumed from RabbitMQ.
 *
 * <p>{@code eventId} is the idempotency key: producers should send a stable, unique ID
 * per business event so redelivery does not create duplicate notifications.
 * Unknown properties are ignored so producers can evolve their payloads without
 * breaking this consumer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface DomainEvent {
    String getEventId();
}
