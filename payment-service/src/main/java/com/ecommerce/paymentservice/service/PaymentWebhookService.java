package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.event.EventPublisher;
import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Processes verified Stripe webhook events and drives the async payment state machine:
 * {@code checkout.session.completed} flips a PENDING payment to SUCCESS and publishes
 * {@code payment.succeeded}; {@code checkout.session.expired} /
 * {@code checkout.session.async_payment_failed} flip it to FAILED and publish
 * {@code payment.failed}. Every transition requires the payment to be PENDING, so repeated
 * Stripe deliveries for a terminal payment are silent no-ops (idempotent).
 */
@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookService.class);

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void handle(Event event) {
        switch (event.getType()) {
            case "checkout.session.completed" -> onSucceeded(event);
            case "checkout.session.expired",
                 "checkout.session.async_payment_failed" -> onFailed(event);
            default -> log.debug("Ignoring unrelated Stripe event type={}", event.getType());
        }
    }

    private void onSucceeded(Event event) {
        Payment payment = resolvePendingPayment(event);
        if (payment == null) {
            return;
        }
        payment.setStatus("SUCCESS");
        Session session = extractSession(event);
        String txnId = session == null ? null : session.getId();
        payment.setTransactionId(txnId);
        Payment savedPayment = paymentRepository.save(payment);

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", "payment-succeeded-" + savedPayment.getOrderId());
        payload.put("orderId", savedPayment.getOrderId());
        payload.put("amount", savedPayment.getAmount());
        payload.put("transactionId", savedPayment.getTransactionId());
        eventPublisher.publish(EventPublisher.RK_PAYMENT_SUCCEEDED, payload);
    }

    private void onFailed(Event event) {
        Payment payment = resolvePendingPayment(event);
        if (payment == null) {
            return;
        }
        String failureReason = "checkout.session.expired".equals(event.getType())
                ? "Checkout session expired"
                : "Asynchronous payment failed";
        payment.setStatus("FAILED");
        payment.setFailureReason(failureReason);
        Payment savedPayment = paymentRepository.save(payment);

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventId", "payment-failed-" + savedPayment.getOrderId());
        payload.put("orderId", savedPayment.getOrderId());
        payload.put("amount", savedPayment.getAmount());
        payload.put("reason", failureReason);
        eventPublisher.publish(EventPublisher.RK_PAYMENT_FAILED, payload);
    }

    /**
     * Extracts the orderId from the Stripe session metadata, loads the matching payment,
     * and returns it only when it is still PENDING. Returns {@code null} (no-op) when the
     * session/metadata cannot be resolved or the payment is absent / not PENDING.
     */
    private Payment resolvePendingPayment(Event event) {
        Session session = extractSession(event);
        if (session == null) {
            log.warn("Stripe event {} carried no deserializable Checkout Session; ignoring.", event.getId());
            return null;
        }
        String orderId = session.getMetadata() == null ? null : session.getMetadata().get("orderId");
        if (orderId == null) {
            log.warn("Stripe session {} has no orderId metadata; cannot correlate.", session.getId());
            return null;
        }
        Payment payment = paymentRepository.findByOrderId(Long.valueOf(orderId)).orElse(null);
        if (payment == null || !"PENDING".equals(payment.getStatus())) {
            log.debug("No PENDING payment for orderId={}; treating webhook as idempotent no-op.", orderId);
            return null;
        }
        return payment;
    }

    private Session extractSession(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        return deserializer.getObject()
                .filter(obj -> obj instanceof Session)
                .map(obj -> (Session) obj)
                .orElse(null);
    }
}
