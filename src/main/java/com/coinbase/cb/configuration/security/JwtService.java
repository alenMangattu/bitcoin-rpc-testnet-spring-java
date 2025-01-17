package com.coinbase.cb.configuration.security;

import com.coinbase.cb.models.DbTables.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtTokenProvider tokenProvider;
    private final JwtTokenValidator tokenValidator;

    public JwtService(JwtTokenProvider tokenProvider, JwtTokenValidator tokenValidator) {
        this.tokenProvider = tokenProvider;
        this.tokenValidator = tokenValidator;
    }

    public String generateToken(User user) {
        return tokenProvider.generateToken(user);
    }

    public boolean validateJwtToken(String token) {
        return tokenValidator.validateToken(token);
    }

    public String getEmailFromToken(String token) {
        String rawEmail = tokenValidator.getEmailFromToken(token);
        try {
            JsonNode jsonNode = objectMapper.readTree(rawEmail);
            return jsonNode.get("email").asText();
        } catch (Exception e) {
            logger.error("Error parsing email from token: " + e.getMessage());
            return rawEmail; // Return raw email if parsing fails
        }
    }
}
