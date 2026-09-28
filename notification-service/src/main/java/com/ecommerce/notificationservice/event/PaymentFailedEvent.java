package com.ecommerce.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Published by order/payment-service on a failed payment (routing key payment.failed). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentFailedEvent implements DomainEvent {
    private String eventId;
    private Long orderId;
    private Long userId;
    private String email;
    private Double amount;
    private String reason;

    @Override
    public String getEventId() {
        return eventId;
    }
}
