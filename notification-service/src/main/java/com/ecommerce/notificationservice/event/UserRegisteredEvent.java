package com.ecommerce.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Published by user-service after a successful registration (routing key user.registered). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRegisteredEvent implements DomainEvent {
    private String eventId;
    private Long userId;
    private String email;
    private String name;

    @Override
    public String getEventId() {
        return eventId;
    }
}
