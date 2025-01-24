package com.coinbase.cb.configuration;

import com.coinbase.cb.configuration.ratelimit.RateLimitRule;
import com.coinbase.cb.configuration.ratelimit.TimeUnit;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RateLimitConfig {
    private final List<RateLimitRule> rules = new ArrayList<>();

    public RateLimitConfig endpoint(String path, int limit, TimeUnit timeUnit) {
        return endpoint(path, null, limit, timeUnit, true);
    }

    public RateLimitConfig endpoint(String path, int limit, TimeUnit timeUnit, boolean perUser) {
        return endpoint(path, null, limit, timeUnit, perUser);
    }

    public RateLimitConfig endpoint(String path, HttpMethod method, int limit, TimeUnit timeUnit) {
        return endpoint(path, method, limit, timeUnit, true);
    }

    public RateLimitConfig endpoint(String path, HttpMethod method, int limit, TimeUnit timeUnit, boolean perUser) {
        rules.add(new RateLimitRule(path, method, limit, timeUnit, perUser));
        return this;
    }

    public List<RateLimitRule> getRules() {
        return rules;
    }
}
