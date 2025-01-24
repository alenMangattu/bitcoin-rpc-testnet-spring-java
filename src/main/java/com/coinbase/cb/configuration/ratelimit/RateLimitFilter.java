package com.coinbase.cb.configuration.ratelimit;

import com.coinbase.cb.configuration.RateLimitConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitConfig rateLimitConfig;
    private final RateLimiter rateLimiter;

    public RateLimitFilter(RateLimitConfig rateLimitConfig, RateLimiter rateLimiter) {
        this.rateLimitConfig = rateLimitConfig;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        Optional<RateLimitRule> matchingRule = rateLimitConfig.getRules().stream()
                .filter(rule -> path.startsWith(rule.getPath()) &&
                        (rule.getMethod() == null || rule.getMethod() == method))
                .findFirst();

        if (matchingRule.isPresent()) {
            RateLimitRule rule = matchingRule.get();
            String identifier = rule.isPerUser() ? 
                    Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                            .map(auth -> auth.getName())
                            .orElse("anonymous") : 
                    "global";

            if (!rateLimiter.tryAcquire(rule, identifier)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                JSONObject jsonResponse = new JSONObject();
                jsonResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
                jsonResponse.put("error", "Rate limit exceeded");
                jsonResponse.put("message", "Too many requests");
                jsonResponse.put("remainingAttempts", rateLimiter.getRemainingAttempts(rule, identifier));
                jsonResponse.put("resetTime", rateLimiter.getResetTime(rule, identifier));

                response.getWriter().write(jsonResponse.toString());
                return;
            }

            response.setHeader("X-RateLimit-Remaining", 
                    String.valueOf(rateLimiter.getRemainingAttempts(rule, identifier)));
            response.setHeader("X-RateLimit-Reset", 
                    String.valueOf(rateLimiter.getResetTime(rule, identifier)));
        }

        filterChain.doFilter(request, response);
    }
} 
