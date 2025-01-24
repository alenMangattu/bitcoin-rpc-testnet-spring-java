package com.coinbase.cb.configuration.ratelimit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis-based rate limiter implementation for controlling API request rates.
 * Uses Redis to track and enforce rate limits across distributed systems.
 */
@Service
public class RateLimiter {
    private final RedisTemplate<String, String> redisTemplate;

    public RateLimiter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Attempts to acquire a rate limit permit for the given rule and identifier.
     * 
     * @param rule The rate limit rule to apply
     * @param identifier The unique identifier (e.g., user ID, IP address) to track
     * @return true if the request is allowed, false if the rate limit is exceeded
     */
    public boolean tryAcquire(RateLimitRule rule, String identifier) {
        String key = rule.generateKey(identifier);
        Long currentCount = redisTemplate.opsForValue().increment(key);
        
        // Set expiration only on first request to ensure consistent time window
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(key, rule.getTimeUnit().getSeconds(), TimeUnit.SECONDS);
        }
        
        return currentCount != null && currentCount <= rule.getLimit();
    }

    /**
     * Gets the number of remaining attempts allowed for the given rule and identifier.
     * 
     * @param rule The rate limit rule to check
     * @param identifier The unique identifier to check
     * @return The number of remaining attempts allowed
     */
    public long getRemainingAttempts(RateLimitRule rule, String identifier) {
        String key = rule.generateKey(identifier);
        String value = redisTemplate.opsForValue().get(key);
        long currentCount = value != null ? Long.parseLong(value) : 0;
        return Math.max(0, rule.getLimit() - currentCount);
    }

    /**
     * Gets the time remaining until the rate limit resets.
     * 
     * @param rule The rate limit rule to check
     * @param identifier The unique identifier to check
     * @return The number of seconds until the rate limit resets
     */
    public long getResetTime(RateLimitRule rule, String identifier) {
        String key = rule.generateKey(identifier);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }
} 