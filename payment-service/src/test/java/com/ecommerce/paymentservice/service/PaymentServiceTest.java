package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.event.EventPublisher;
import com.ecommerce.paymentservice.model.Payment;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.service.gateway.MockGateway;
import com.ecommerce.paymentservice.service.gateway.PaymentGatewaySelector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private PaymentGatewaySelector gatewaySelector;

    private PaymentService service;

    @BeforeEach
    void setUp() {
        lenient().when(gatewaySelector.select()).thenReturn(new MockGateway());
        service = new PaymentService(paymentRepository, eventPublisher, gatewaySelector);
    }

    private Payment newPayment(Long orderId, Double amount, String method) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        return payment;
    }

    @Test
    void newSuccessfulPaymentUsesMockOutcomeAndPublishesEvent() {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = service.process(newPayment(1L, 100.0, "upi"));

        assertEquals("SUCCESS", result.getStatus());
        assertTrue(result.getTransactionId().startsWith("MOCK-"));
        assertEquals("UPI", result.getPaymentMethod());
        verify(paymentRepository).save(any(Payment.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(eventPublisher).publish(eq(EventPublisher.RK_PAYMENT_SUCCEEDED), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals("payment-succeeded-1", payload.get("eventId"));
        assertEquals(1L, payload.get("orderId"));
        assertEquals(100.0, payload.get("amount"));
        assertEquals(result.getTransactionId(), payload.get("transactionId"));
    }

    @Test
    void nullOrderIdRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.process(newPayment(null, 100.0, "UPI")));
        assertEquals("Order ID is required.", ex.getMessage());
    }

    @Test
    void nullAmountRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.process(newPayment(1L, null, "UPI")));
        assertEquals("Payment amount must be greater than zero.", ex.getMessage());
    }

    @Test
    void zeroAmountRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.process(newPayment(1L, 0.0, "UPI")));
        assertEquals("Payment amount must be greater than zero.", ex.getMessage());
    }

    @Test
    void negativeAmountRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.process(newPayment(1L, -5.0, "UPI")));
        assertEquals("Payment amount must be greater than zero.", ex.getMessage());
    }

    @Test
    void unsupportedMethodRejectedWithRawValue() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.process(newPayment(1L, 100.0, "crypto")));
        assertEquals("Unsupported payment method: crypto", ex.getMessage());
    }

    @Test
    void idempotentReturnsExistingWithoutEvent() {
        Payment existing = newPayment(1L, 100.0, "UPI");
        existing.setStatus("SUCCESS");
        existing.setTransactionId("MOCK-existing");
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(existing));

        Payment result = service.process(newPayment(1L, 100.0, "UPI"));

        assertEquals(existing, result);
        verify(eventPublisher, never()).publish(any(), any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void differingPaymentRejected() {
        Payment existing = newPayment(1L, 100.0, "UPI");
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(existing));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.process(newPayment(1L, 200.0, "UPI")));
        assertEquals("A different payment already exists for this order.", ex.getMessage());
    }
}
