package com.ecommerce.paymentservice.service.gateway;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves the active {@link PaymentGateway} from the {@code payment.gateway}
 * property (default {@code "mock"}), matching gateways by their {@link PaymentGateway#name()}.
 *
 * <p>If the configured key is unknown or missing, {@link #select()} logs a warning
 * and falls back to the mock gateway. It never fails startup, never throws, and
 * never returns {@code null} — the mock gateway works out of the box.</p>
 */
@Component
@RequiredArgsConstructor
public class PaymentGatewaySelector {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewaySelector.class);
    private static final String MOCK = "mock";

    private final List<PaymentGateway> gateways;

    @Value("${payment.gateway:mock}")
    private String configuredGateway;

    private final Map<String, PaymentGateway> gatewaysByName = new HashMap<>();

    @PostConstruct
    void init() {
        for (PaymentGateway gateway : gateways) {
            gatewaysByName.put(gateway.name().toLowerCase(Locale.ROOT), gateway);
        }
    }

    /**
     * Selects the configured gateway, falling back to the mock gateway when the
     * configured key is unknown or missing.
     *
     * @return the active {@link PaymentGateway}; never {@code null}
     */
    public PaymentGateway select() {
        String key = configuredGateway == null ? MOCK : configuredGateway.trim().toLowerCase(Locale.ROOT);
        PaymentGateway gateway = gatewaysByName.get(key);
        if (gateway != null) {
            return gateway;
        }
        log.warn("Configured payment.gateway='{}' not found; falling back to mock.", configuredGateway);
        return gatewaysByName.get(MOCK);
    }
}
