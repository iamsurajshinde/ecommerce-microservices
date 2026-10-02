package com.ecommerce.paymentservice.service.gateway;

import com.ecommerce.paymentservice.model.Payment;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentLink;
import com.stripe.model.Price;
import com.stripe.param.PaymentLinkCreateParams;
import com.stripe.param.PriceCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Stripe gateway. Synchronous {@code charge(...)} is unsupported (Stripe is async-only);
 * the asynchronous checkout path uses {@link #createPaymentLink(Payment)} to produce a
 * hosted Stripe PaymentLink URL. The no-arg constructor is preserved; secrets are injected
 * by field injection so existing {@code new StripeGateway()} test constructions keep working.
 */
@Component
public class StripeGateway implements PaymentGateway {

    @Value("${stripe.secret.key:}")
    private String stripeSecretKey;

    @Value("${stripe.currency:usd}")
    private String currency;

    @Value("${stripe.success-url:https://example.com/checkout/success}")
    private String successUrl;

    @Override
    public GatewayResult charge(Payment payment) {
        throw new UnsupportedOperationException("Stripe gateway is not configured.");
    }

    @Override
    public String createPaymentLink(Payment payment) {
        try {
            Stripe.apiKey = stripeSecretKey;
            long unitAmount = Math.round(payment.getAmount() * 100);

            Price price = Price.create(
                    PriceCreateParams.builder()
                            .setCurrency(currency.toLowerCase(Locale.ROOT))
                            .setUnitAmount(unitAmount)
                            .setProductData(
                                    PriceCreateParams.ProductData.builder()
                                            .setName("Order #" + payment.getOrderId())
                                            .build())
                            .build());

            PaymentLink paymentLink = PaymentLink.create(
                    PaymentLinkCreateParams.builder()
                            .addLineItem(
                                    PaymentLinkCreateParams.LineItem.builder()
                                            .setPrice(price.getId())
                                            .setQuantity(1L)
                                            .build())
                            .putMetadata("orderId", String.valueOf(payment.getOrderId()))
                            .setAfterCompletion(
                                    PaymentLinkCreateParams.AfterCompletion.builder()
                                            .setType(PaymentLinkCreateParams.AfterCompletion.Type.REDIRECT)
                                            .setRedirect(
                                                    PaymentLinkCreateParams.AfterCompletion.Redirect.builder()
                                                            .setUrl(successUrl + "?orderId=" + payment.getOrderId())
                                                            .build())
                                            .build())
                            .build());

            return paymentLink.getUrl();
        } catch (StripeException exception) {
            throw new PaymentGatewayException(
                    "Stripe payment link creation failed: " + exception.getMessage(), exception);
        }
    }

    @Override
    public String name() {
        return "stripe";
    }
}
