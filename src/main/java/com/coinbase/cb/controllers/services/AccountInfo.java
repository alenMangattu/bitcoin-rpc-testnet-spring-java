package com.coinbase.cb.controllers.services;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import com.coinbase.cb.controllers.Query;
import org.springframework.stereotype.Service;
import com.coinbase.cb.models.DbTables.User;
import com.coinbase.cb.configuration.security.CustomUserDetails;
import org.springframework.cache.annotation.CachePut;
import org.springframework.transaction.annotation.Transactional;
import com.coinbase.cb.repository.UserRespository;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;

@Service
public class AccountInfo implements Query<Void, Map<String, Object>> {
    private final UserRespository userRepository;

    public AccountInfo(UserRespository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> execute(Void input) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();
            
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

    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> executeWithId(Long userId) {
        try {
            // Get current authenticated user's details
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String currentUserEmail = userDetails.getUsername();
            
            // Get requested user's information
            User requestedUser = userRepository.findByIdWithWallet(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("userId", requestedUser.getUser_id());
            response.put("fullName", requestedUser.getFirstName() + " " + requestedUser.getLastName());
            
            // If the user is requesting their own information or if they're authenticated
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
                // For other users, only show wallet address
                if (requestedUser.getWallet() != null) {
                    response.put("walletAddress", requestedUser.getWallet().getAddress());
                } else {
                    response.put("walletAddress", null);
                }
            }

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

    @CachePut(value = "userCache", key = "#email")
    private String cacheUserInfo(String email, String userInfo) {
        return userInfo;
    }
}
