package com.coinbase.cb.configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class RateLimitConfig {

    public static Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(1) // 1 request
            .refillGreedy(1, Duration.ofMinutes(1)) // Refill 1 token every minute
            .build();

        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
} 