package com.ecommerce.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Published by payment-service on a successful payment (routing key payment.succeeded). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentSucceededEvent implements DomainEvent {
    private String eventId;
    private Long orderId;
    private Long userId;
    private String email;
    private Double amount;
    private String transactionId;

    @Override
    public String getEventId() {
        return eventId;
    }
}
