package com.ecommerce.paymentservice.service.gateway;

import com.ecommerce.paymentservice.model.Payment;

/**
 * Strategy for computing the synchronous OUTCOME of charging a payment.
 *
 * <p>Implementations compute only the charge outcome (success/status/transactionId).
 * Validation, idempotency, persistence, and event publishing are owned by the
 * caller ({@code PaymentService}), never by a gateway.</p>
 */
public interface PaymentGateway {

    /**
     * Computes the charge outcome for the given payment.
     *
     * @param payment the payment to charge (already validated by the caller)
     * @return the resulting {@link GatewayResult}
     */
    GatewayResult charge(Payment payment);

    /**
     * The lowercase key identifying this gateway (e.g. {@code "mock"}, {@code "stripe"}, {@code "razorpay"}).
     *
     * @return the gateway name key
     */
    String name();

    /**
     * Creates a hosted payment-link / checkout URL for an asynchronous payment.
     * Default: unsupported. Only async gateways (Stripe) override this.
     *
     * @param payment the PENDING payment (validated by the caller)
     * @return the hosted checkout URL the buyer is redirected to
     */
    default String createPaymentLink(Payment payment) {
        throw new UnsupportedOperationException(name() + " does not support payment links.");
    }
}
