package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", configuration = FeignConfig.class)
public interface PaymentClient {

    @PostMapping("/api/payments/process")
    PaymentDTO processPayment(@RequestBody PaymentRequest request);
}
