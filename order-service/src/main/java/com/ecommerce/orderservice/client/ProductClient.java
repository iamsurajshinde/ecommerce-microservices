package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", configuration = FeignConfig.class)
public interface ProductClient {
    @GetMapping("/api/products/{id}")
    ProductDTO getProductById(@PathVariable("id") Long id);

    @PatchMapping("/api/products/{id}/stock")
    ProductDTO decreaseStock(
            @PathVariable("id") Long id,
            @RequestParam("quantity") Integer quantity);

    @PatchMapping("/api/products/{id}/stock/restore")
    ProductDTO restoreStock(
            @PathVariable("id") Long id,
            @RequestParam("quantity") Integer quantity);
}