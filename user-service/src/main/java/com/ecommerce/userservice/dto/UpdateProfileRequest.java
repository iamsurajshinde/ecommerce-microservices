package com.ecommerce.userservice.dto;

public record UpdateProfileRequest(
        String name,
        String email,
        String password
) {
}
