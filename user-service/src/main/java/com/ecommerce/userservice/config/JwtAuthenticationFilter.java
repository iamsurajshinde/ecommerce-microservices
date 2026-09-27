package com.ecommerce.userservice.config;

import com.ecommerce.userservice.model.User;
import com.ecommerce.userservice.service.JwtService;
import com.ecommerce.userservice.service.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            String email = null;

            try {
                email = jwtService.extractEmail(token);
            } catch (JwtException | IllegalArgumentException exception) {
                // Invalid tokens are rejected by authorization when no authentication is established.
            }

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userService.getUserByEmail(userService.normalizeEmail(email));
                if (user != null && jwtService.isValid(token, user)) {
                    authenticate(user);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(User user) {
        String role = user.getRole() == null || user.getRole().isBlank()
                ? "USER"
                : user.getRole().trim().toUpperCase(Locale.ROOT);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
