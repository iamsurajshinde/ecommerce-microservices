package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.service.ProductService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    @Value("${internal.service-token}")
    private String internalServiceToken;

    @GetMapping
    public List<Product> getAllProducts(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String category) {
        return productService.findProducts(keyword, category);
    }

    @GetMapping("/category/{categoryId}")
    public List<Product> getProductsByCategory(@PathVariable Long categoryId) {
        return productService.findByCategoryId(categoryId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productService.create(product);
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return productService.update(id, product);
    }

    @PatchMapping("/{id}/stock")
    public Product decreaseStock(
            @PathVariable Long id,
            @RequestParam Integer quantity,
            @RequestHeader("X-Internal-Service-Token") String serviceToken,
            Authentication authentication) {
        if (!internalServiceToken.equals(serviceToken)
                || authentication == null
                || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Internal service access required.");
        }
        return productService.decreaseStock(id, quantity);
    }

    @PatchMapping("/{id}/stock/restore")
    public Product restoreStock(
            @PathVariable Long id,
            @RequestParam Integer quantity,
            @RequestHeader("X-Internal-Service-Token") String serviceToken,
            Authentication authentication) {
        if (!internalServiceToken.equals(serviceToken)
                || authentication == null
                || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Internal service access required.");
        }
        return productService.increaseStock(id, quantity);
    }
}