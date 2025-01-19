package com.coinbase.cb.configuration.security;

import com.coinbase.cb.models.DbTables.User;
import com.coinbase.cb.repository.UserRespository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRespository userRepository;
    private final RedisTemplate<String, User> redisTemplate;
    private static final String USER_CACHE_PREFIX = "user:";
    private static final long CACHE_DURATION = 3600; // 1 hour in seconds

    public CustomUserDetailsService(UserRespository userRepository, RedisTemplate<String, User> redisTemplate) {
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Try to get from cache first
        User user = redisTemplate.opsForValue().get(USER_CACHE_PREFIX + email);
        
        if (user == null) {
            // If not in cache, get from database
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
            
            // Cache the user
            redisTemplate.opsForValue().set(USER_CACHE_PREFIX + email, user, CACHE_DURATION, TimeUnit.SECONDS);
        }

        return new CustomUserDetails(user);
    }

    public void clearUserCache(String email) {
        redisTemplate.delete(USER_CACHE_PREFIX + email);
    }
} 