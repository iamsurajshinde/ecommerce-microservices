package com.ecommerce.paymentservice.service.gateway;

import com.ecommerce.paymentservice.model.Payment;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockGatewayTest {

    private final MockGateway gateway = new MockGateway();

    @Test
    void nameIsMock() {
        assertEquals("mock", gateway.name());
    }

    @Test
    void chargeProducesSuccessfulMockResult() {
        GatewayResult result = gateway.charge(new Payment());

        assertTrue(result.success());
        assertEquals("SUCCESS", result.status());
        assertTrue(result.transactionId().startsWith("MOCK-"));
        String uuidPart = result.transactionId().substring("MOCK-".length());
        assertDoesNotThrow(() -> UUID.fromString(uuidPart));
    }
}
