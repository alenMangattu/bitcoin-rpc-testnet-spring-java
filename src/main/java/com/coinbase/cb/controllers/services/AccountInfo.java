package com.coinbase.cb.controllers.services;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import com.coinbase.cb.controllers.Query;
import org.springframework.stereotype.Service;
import com.coinbase.cb.models.DbTables.User;
import com.coinbase.cb.configuration.security.CustomUserDetails;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import com.coinbase.cb.repository.UserRespository;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;

@Service
public class AccountInfo implements Query<Void, Map<String, Object>> {
    private final UserRespository userRepository;
    private static final String ACCOUNT_CACHE_NAME = "account-info";

    public AccountInfo(UserRespository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> execute(Void input) {
        try {
            Map<String, Object> response = getAccountInfo(getCurrentUserEmail());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "An error occurred while fetching user information");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Cacheable(value = ACCOUNT_CACHE_NAME, key = "#email")
    protected Map<String, Object> getAccountInfo(String email) {
        User user = userRepository.findByEmailWithWallet(email)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getUser_id());
        response.put("email", email);
        response.put("fullName", user.getFirstName() + " " + user.getLastName());
        
        if (user.getWallet() != null) {
            response.put("walletAddress", user.getWallet().getAddress());
            response.put("walletBalance", user.getWallet().getBalance());
        } else {
            response.put("walletAddress", null);
            response.put("walletBalance", 0);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> executeWithId(Long userId) {
        try {
            Map<String, Object> response = getAccountInfoById(userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "An error occurred while fetching user information");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Cacheable(value = ACCOUNT_CACHE_NAME, key = "#userId")
    protected Map<String, Object> getAccountInfoById(Long userId) {
        String currentUserEmail = getCurrentUserEmail();
        
        User requestedUser = userRepository.findByIdWithWallet(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("userId", requestedUser.getUser_id());
        response.put("fullName", requestedUser.getFirstName() + " " + requestedUser.getLastName());
        
        if (requestedUser.getEmail().equals(currentUserEmail)) {
            response.put("email", requestedUser.getEmail());
            if (requestedUser.getWallet() != null) {
                response.put("walletAddress", requestedUser.getWallet().getAddress());
                response.put("walletBalance", requestedUser.getWallet().getBalance());
            } else {
                response.put("walletAddress", null);
                response.put("walletBalance", 0);
            }
        } else {
            if (requestedUser.getWallet() != null) {
                response.put("walletAddress", requestedUser.getWallet().getAddress());
            } else {
                response.put("walletAddress", null);
            }
        }

        return response;
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }

    @CacheEvict(value = ACCOUNT_CACHE_NAME, key = "#email")
    public void clearAccountCache(String email) {
        // This method is intentionally empty as @CacheEvict handles the cache eviction
    }

    @CacheEvict(value = ACCOUNT_CACHE_NAME, key = "#userId")
    public void clearAccountCacheById(Long userId) {
        // This method is intentionally empty as @CacheEvict handles the cache eviction
    }
}
