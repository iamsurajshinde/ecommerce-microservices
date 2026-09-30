package com.ecommerce.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Published by order-service when an order is cancelled (routing key order.cancelled). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCancelledEvent implements DomainEvent {
    private String eventId;
    private Long orderId;
    private Long userId;
    private String email;
    private Boolean refundIssued;

    @Override
    public String getEventId() {
        return eventId;
    }
}
