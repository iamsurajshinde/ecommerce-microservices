package com.ecommerce.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Published by product-service when stock crosses a threshold (routing key product.low_stock). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LowStockEvent implements DomainEvent {
    private String eventId;
    private Long productId;
    private String productName;
    private Integer remainingStock;

    @Override
    public String getEventId() {
        return eventId;
    }
}
