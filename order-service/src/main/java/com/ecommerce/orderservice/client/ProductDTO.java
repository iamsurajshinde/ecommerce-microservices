package com.ecommerce.orderservice.client;

public record ProductDTO(Long id, String name, Double price, Integer stockQuantity) {
}
