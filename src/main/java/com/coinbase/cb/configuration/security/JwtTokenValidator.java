package com.coinbase.cb.configuration.security;

import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenValidator {
    
    private final JwtTokenProvider tokenProvider;

    public JwtTokenValidator(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = tokenProvider.parseToken(token);
            
            // Check if token has expired
            if (claims.getExpiration().before(new Date())) {
                return false;
            }
            
            // Check if token wasn't issued in the future
            Date issuedAt = claims.getIssuedAt();
            if (issuedAt == null || issuedAt.after(new Date())) {
                return false;
            }
            
            // Ensure token has a subject
            return claims.getSubject() != null;
            
        } catch (Exception e) {
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        Claims claims = tokenProvider.parseToken(token);
        return claims.getSubject();
    }
} 