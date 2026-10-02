package com.ecommerce.paymentservice.service.gateway;

import com.ecommerce.paymentservice.model.Payment;
import org.springframework.stereotype.Component;

/**
 * Inactive scaffold for a future Razorpay integration. Never active under the
 * default {@code payment.gateway: mock} configuration. No SDK, no network calls.
 */
@Component
public class RazorpayGateway implements PaymentGateway {

    @Override
    public GatewayResult charge(Payment payment) {
        // Real synchronous Razorpay order/capture wiring would go here,
        // mapping the Razorpay response onto a GatewayResult. Not implemented yet.
        throw new UnsupportedOperationException("Razorpay gateway is not configured.");
    }

    @Override
    public String name() {
        return "razorpay";
    }
}
