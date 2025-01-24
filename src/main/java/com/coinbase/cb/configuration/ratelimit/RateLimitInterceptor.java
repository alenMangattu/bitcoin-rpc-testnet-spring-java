package com.coinbase.cb.configuration.ratelimit;

import com.coinbase.cb.configuration.RateLimitConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.json.JSONObject;

/**
 * Interceptor that enforces rate limits on configured endpoints.
 * Integrates with Spring Security to support per-user rate limiting.
 * Uses Redis for distributed rate limiting across multiple application instances.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimiter rateLimiter;
    private final RateLimitConfig rateLimitConfig;

    public RateLimitInterceptor(RateLimiter rateLimiter, RateLimitConfig rateLimitConfig) {
        this.rateLimiter = rateLimiter;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        // Get authenticated user or use "anonymous" for unauthenticated requests
        String identifier = SecurityContextHolder.getContext().getAuthentication() != null ?
                SecurityContextHolder.getContext().getAuthentication().getName() : "anonymous";

        // Find the first matching rate limit rule for this request
        RateLimitRule matchingRule = rateLimitConfig.getRules().stream()
                .filter(rule -> rule.matches(path))
                .findFirst()
                .orElse(null);

        if (matchingRule != null) {
            // Try to acquire a rate limit token
            if (!rateLimiter.tryAcquire(matchingRule, identifier)) {
                // If rate limit exceeded, return 429 Too Many Requests
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
                jsonResponse.put("error", "Rate limit exceeded");
                jsonResponse.put("message", "Too many requests");
                jsonResponse.put("remainingAttempts", rateLimiter.getRemainingAttempts(matchingRule, identifier));
                jsonResponse.put("resetTime", rateLimiter.getResetTime(matchingRule, identifier));

                response.getWriter().write(jsonResponse.toString());
                return false;
            }

            // Add rate limit headers to response
            response.setHeader("X-RateLimit-Remaining", 
                    String.valueOf(rateLimiter.getRemainingAttempts(matchingRule, identifier)));
            response.setHeader("X-RateLimit-Reset", 
                    String.valueOf(rateLimiter.getResetTime(matchingRule, identifier)));
        }

        return true;
    }
} 
