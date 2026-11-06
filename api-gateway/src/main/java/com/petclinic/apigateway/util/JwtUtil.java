package com.petclinic.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    public Claims validateTokenAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Fail at startup: a secret that is not base64, or shorter than 256 bits, passes docker
     * compose's "is it set" check but makes the gateway reject every secured request with 401.
     */
    @PostConstruct
    void validateSecret() {
        try {
            getSignInKey();
        } catch (RuntimeException e) {
            throw new IllegalStateException("JWT_SECRET must be base64 of at least 32 random bytes "
                    + "(generate one with: openssl rand -base64 32)", e);
        }
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
