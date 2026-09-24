package com.ecommerce.userservice.dto;

public record LoginResponse(String token, Long userId, String email, String role) {
}
