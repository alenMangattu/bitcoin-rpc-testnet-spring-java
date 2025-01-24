package com.coinbase.cb.configuration;

import com.coinbase.cb.configuration.ratelimit.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * Configuration class for rate limiting endpoints.
 * Similar to Spring Security's authorizeHttpRequests, this allows defining rate limits per endpoint.
 */
@Configuration
public class RateLimitConfiguration {

    /**
     * Configures rate limits for different endpoints.
     * Format: endpoint(path, [httpMethod], limit, timeUnit, [perUser])
     * - path: Ant-style pattern for matching URLs (e.g., /account/** matches all /account paths)
     * - httpMethod: Optional HTTP method to limit (null means all methods)
     * - limit: Number of requests allowed
     * - timeUnit: Time window (SECONDS, MINUTES, HOURS)
     * - perUser: Optional boolean, true for per-user limits, false for global limits (defaults to true)
     */
    @Bean
    public RateLimitConfig rateLimitConfig() {
        return new RateLimitConfig()
            // Allow 8 requests per minute for all /account endpoints (including /account/id, /account/, etc.)
            .endpoint("/account/**", 8, TimeUnit.MINUTES)
            // Allow 2 POST requests per minute for the /send endpoint
            .endpoint("/send", HttpMethod.POST, 2, TimeUnit.MINUTES);
            // Example of a global rate limit (uncomment if needed):
            // .endpoint("/api/v1/auth", 10, TimeUnit.MINUTES, false); // Global limit for all users combined
    }
} 