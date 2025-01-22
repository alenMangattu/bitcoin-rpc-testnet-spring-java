package com.coinbase.cb.configuration;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.json.JSONObject;
import org.springframework.lang.NonNull;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final ProxyManager<String> proxyManager;

    public RateLimitInterceptor(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        if (!request.getRequestURI().startsWith("/account")) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("status", HttpStatus.UNAUTHORIZED.value());
            jsonResponse.put("error", "Unauthorized");
            jsonResponse.put("message", "Authentication required for accessing account information");
            jsonResponse.put("path", request.getRequestURI());
            
            response.getWriter().write(jsonResponse.toString());
            return false;
        }

        String username = authentication.getName();
        Bucket bucket = proxyManager.builder().build(username, () -> RateLimitConfig.getDefaultBucketConfiguration());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
            
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
            jsonResponse.put("error", "Too Many Requests");
            jsonResponse.put("message", "Rate limit exceeded. Only 1 request per minute is allowed for account endpoints.");
            jsonResponse.put("retryAfter", waitForRefill);
            jsonResponse.put("path", request.getRequestURI());
            
            response.getWriter().write(jsonResponse.toString());
            return false;
        }
    }
} 