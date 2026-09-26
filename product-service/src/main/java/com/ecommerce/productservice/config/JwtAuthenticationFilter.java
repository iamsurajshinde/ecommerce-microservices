package com.ecommerce.productservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request.getHeader("Authorization"));
        if (token != null && jwtService.isValid(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "jwt-user", null, Collections.emptyList()));
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }

        String value = authorization.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            value = value.substring(7).trim();
        }
        return value.isBlank() ? null : value;
    }
}
