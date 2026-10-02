package com.ecommerce.orderservice.consumer;

import com.ecommerce.orderservice.config.RabbitConsumerConfig;
import com.ecommerce.orderservice.event.PaymentFailedEvent;
import com.ecommerce.orderservice.event.PaymentSucceededEvent;
import com.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code payment.succeeded} / {@code payment.failed} events from the order
 * service's own queue and drives the idempotent order transitions. The payload is
 * resolved to a typed event by the {@code __TypeId__} header via the container factory
 * configured in {@link RabbitConsumerConfig}.
 */
@Component
@RequiredArgsConstructor
@RabbitListener(queues = RabbitConsumerConfig.ORDER_PAYMENTS_QUEUE,
        containerFactory = "paymentListenerContainerFactory")
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final OrderService orderService;

    @RabbitHandler
    public void on(PaymentSucceededEvent e) {
        orderService.confirmFromPayment(e.getOrderId());
    }

    @RabbitHandler
    public void on(PaymentFailedEvent e) {
        orderService.failFromPayment(e.getOrderId(), e.getReason());
    }

    @RabbitHandler(isDefault = true)
    public void onUnknown(Object m) {
        log.warn("Received unexpected payment event payload: {}", m);
    }
}
