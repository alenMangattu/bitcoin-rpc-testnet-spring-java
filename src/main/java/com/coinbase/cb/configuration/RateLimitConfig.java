package com.coinbase.cb.configuration;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.grid.jcache.JCacheProxyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.redisson.config.Config;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class RateLimitConfig {

    @Bean
    public RedissonClient redisson() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://localhost:6379");
        return Redisson.create(config);
    }

    @Bean
    public CacheManager rateLimitCacheManager(RedissonClient redissonClient) {
        CacheManager manager = Caching.getCachingProvider().getCacheManager();
        MutableConfiguration<String, byte[]> config = 
            new MutableConfiguration<String, byte[]>()
                .setTypes(String.class, byte[].class)
                .setStatisticsEnabled(true)
                .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.HOURS, 1)));

        // Create cache with Redisson configuration
        manager.createCache("rate-limit-buckets", 
            RedissonConfiguration.fromInstance(redissonClient));
        return manager;
    }

    @Bean
    public ProxyManager<String> proxyManager(CacheManager rateLimitCacheManager) {
        return new JCacheProxyManager<>(rateLimitCacheManager.getCache("rate-limit-buckets"));
    }

    public static BucketConfiguration getDefaultBucketConfiguration() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(1) // 1 request
            .refillGreedy(1, java.time.Duration.ofMinutes(1)) // Refill 1 token every minute
            .build();

        return BucketConfiguration.builder()
            .addLimit(limit)
            .build();
    }
} 