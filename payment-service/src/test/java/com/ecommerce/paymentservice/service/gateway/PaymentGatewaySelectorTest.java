package com.ecommerce.paymentservice.service.gateway;

import com.ecommerce.paymentservice.model.Payment;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;

class PaymentGatewaySelectorTest {

    private final MockGateway mock = new MockGateway();
    private final StripeGateway stripe = new StripeGateway();

    private PaymentGatewaySelector newSelector(String configured) {
        PaymentGatewaySelector selector = new PaymentGatewaySelector(List.of(mock, stripe));
        ReflectionTestUtils.setField(selector, "configuredGateway", configured);
        ReflectionTestUtils.invokeMethod(selector, "init");
        return selector;
    }

    @Test
    void selectsMockWhenConfiguredMock() {
        assertSame(mock, newSelector("mock").select());
    }

    @Test
    void selectsNamedGatewayWhenConfiguredAndPresent() {
        assertSame(stripe, newSelector("stripe").select());
    }

    @Test
    void fallsBackToMockWhenConfiguredKeyUnknown() {
        PaymentGatewaySelector selector = newSelector("does-not-exist");
        assertDoesNotThrow(selector::select);
        assertSame(mock, selector.select());
    }

    @Test
    void fallsBackToMockWhenConfiguredKeyNull() {
        PaymentGatewaySelector selector = newSelector(null);
        assertSame(mock, selector.select());
    }

    @Test
    void chargeViaSelectedMockSucceeds() {
        GatewayResult result = newSelector("mock").select().charge(new Payment());
        assertSame(true, result.success());
    }
}
