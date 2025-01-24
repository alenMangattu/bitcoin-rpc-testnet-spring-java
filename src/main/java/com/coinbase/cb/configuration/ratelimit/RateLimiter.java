package com.coinbase.cb.configuration.ratelimit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimiter {
    private final RedisTemplate<String, String> redisTemplate;

    public RateLimiter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquire(RateLimitRule rule, String identifier) {
        String key = rule.generateKey(identifier);
        Long currentCount = redisTemplate.opsForValue().increment(key);
        
        if (currentCount == 1) {
            redisTemplate.expire(key, rule.getTimeUnit().getSeconds(), TimeUnit.SECONDS);
        }
        
        return currentCount != null && currentCount <= rule.getLimit();
    }

    public long getRemainingAttempts(RateLimitRule rule, String identifier) {
        String key = rule.generateKey(identifier);
        String value = redisTemplate.opsForValue().get(key);
        long currentCount = value != null ? Long.parseLong(value) : 0;
        return Math.max(0, rule.getLimit() - currentCount);
    }

    public long getResetTime(RateLimitRule rule, String identifier) {
        String key = rule.generateKey(identifier);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }
} 