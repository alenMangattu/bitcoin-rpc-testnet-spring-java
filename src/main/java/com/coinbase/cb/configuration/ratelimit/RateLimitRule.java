package com.coinbase.cb.configuration.ratelimit;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

/**
 * Represents a single rate limiting rule for an endpoint.
 * Each rule defines the path pattern to match, optional HTTP method,
 * number of requests allowed, time window, and whether the limit is per-user or global.
 */
public class RateLimitRule {
    private final String path;              // Ant-style path pattern (e.g., /account/**)
    private final HttpMethod method;        // HTTP method to limit (null means all methods)
    private final int limit;                // Number of requests allowed
    private final TimeUnit timeUnit;        // Time window (SECONDS, MINUTES, HOURS)
    private final boolean perUser;          // Whether limit applies per user or globally
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitRule(String path, HttpMethod method, int limit, TimeUnit timeUnit, boolean perUser) {
        this.path = path;
        this.method = method;
        this.limit = limit;
        this.timeUnit = timeUnit;
        this.perUser = perUser;
    }

    public String getPath() {
        return path;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public int getLimit() {
        return limit;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public boolean isPerUser() {
        return perUser;
    }

    /**
     * Checks if a request path matches this rule's pattern using Spring's AntPathMatcher.
     * Supports wildcards: ? (single char), * (directory), ** (multiple directories)
     */
    public boolean matches(String requestPath) {
        return pathMatcher.match(path, requestPath);
    }

    /**
     * Generates a unique Redis key for rate limiting.
     * Format: rate_limit:path:method:identifier
     * Example: rate_limit:/account/**:GET:user123
     */
    public String generateKey(String identifier) {
        StringBuilder key = new StringBuilder("rate_limit:");
        key.append(path);
        if (method != null) {
            key.append(":").append(method);
        }
        if (perUser && identifier != null) {
            key.append(":").append(identifier);
        }
        return key.toString();
    }
} 