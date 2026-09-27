package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "payment-service", configuration = FeignConfig.class)
public interface PaymentClient {

    @PostMapping("/api/payments/process")
    PaymentDTO processPayment(@RequestBody PaymentRequest request);

    @PostMapping("/api/payments/order/{orderId}/refund")
    PaymentDTO refundPayment(@PathVariable("orderId") Long orderId);
}
