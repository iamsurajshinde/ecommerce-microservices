package com.ecommerce.paymentservice.service.gateway;

import com.ecommerce.paymentservice.model.Payment;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default, always-available gateway that reproduces the historical mock outcome:
 * every charge succeeds with status {@code "SUCCESS"} and a {@code "MOCK-"}-prefixed
 * transaction id. Performs no validation, persistence, or event publishing.
 */
@Component
public class MockGateway implements PaymentGateway {

    @Override
    public GatewayResult charge(Payment payment) {
        return GatewayResult.success("MOCK-" + UUID.randomUUID(), "SUCCESS");
    }

    @Override
    public String name() {
        return "mock";
    }
}
