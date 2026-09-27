package com.ecommerce.orderservice.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public Optional<String> extractSubject(String token) {
        try {
            return Optional.ofNullable(Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token).getPayload().getSubject());
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public Optional<String> extractRole(String token) {
        try {
            return Optional.ofNullable(Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token).getPayload().get("role", String.class));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
