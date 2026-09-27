package com.ecommerce.cartservice.client;

public record ProductDTO(Long id, String name, Double price, Integer stockQuantity) {
}
