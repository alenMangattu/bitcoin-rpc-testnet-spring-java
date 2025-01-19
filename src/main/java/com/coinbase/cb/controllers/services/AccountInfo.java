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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import com.coinbase.cb.repository.UserRespository;
import jakarta.persistence.NoResultException;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;

@Service
public class AccountInfo implements Query<Void, Map<String, Object>> {
    private final UserRespository userRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

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
            
            User user = entityManager.createQuery(
                "SELECT u FROM User u LEFT JOIN FETCH u.wallet w WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getSingleResult();

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
            
        } catch (NoResultException e) {
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
