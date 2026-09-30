package com.ecommerce.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Published by order-service when an order reaches CONFIRMED (routing key order.confirmed). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderConfirmedEvent implements DomainEvent {
    private String eventId;
    private Long orderId;
    private Long userId;
    private String email;
    private Double totalPrice;

    @Override
    public String getEventId() {
        return eventId;
    }
}
