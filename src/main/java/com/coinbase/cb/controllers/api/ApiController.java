package com.coinbase.cb.controllers.api;

import com.coinbase.cb.controllers.api.api_services.LoginInController;
import com.coinbase.cb.controllers.api.api_services.RegisterController;
import com.coinbase.cb.models.DTOs.UserDTO;
import com.coinbase.cb.models.DbTables.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller handling authentication endpoints.
 * These endpoints are public and do not require authentication.
 */
@RestController()
@RequestMapping("/api/v1")
public class ApiController {

    private final RegisterController signUpController;
    private final LoginInController loginController;
    
    public ApiController(RegisterController signUpController, 
                        LoginInController loginController) {
        this.signUpController = signUpController;
        this.loginController = loginController;
    }

    /**
     * Register a new user account.
     * 
     * Endpoint: POST /api/v1/auth/register
     * Authentication: Not required
     * 
     * Request Body:
     * {
     *   "firstName": "string",    // User's first name
     *   "lastName": "string",     // User's last name
     *   "email": "string",        // Valid email address
     *   "password": "string"      // Base64 encoded password
     * }
     * 
     * @param user The user registration details
     * @return UserDTO with account details and JWT token in Authorization header
     * @throws CustomErrorResponse for:
     *         - Invalid input format
     *         - Email already exists
     *         - Wallet creation failure
     */
    @PostMapping("/auth/register")
    public ResponseEntity<UserDTO> signup(@RequestBody User user) {
        return signUpController.execute(user);
    }

    /**
     * Authenticate an existing user.
     * 
     * Endpoint: POST /api/v1/auth/login
     * Authentication: Not required
     * 
     * Request Body:
     * {
     *   "email": "string",        // Registered email address
     *   "password": "string"      // Base64 encoded password
     * }
     * 
     * @param user The login credentials
     * @return UserDTO with account details and JWT token in Authorization header
     * @throws CustomErrorResponse for:
     *         - Invalid credentials
     *         - User not found
     *         - Invalid input format
     */
    @PostMapping("/auth/login")
    public ResponseEntity<UserDTO> login(@RequestBody User user) {
        return loginController.execute(user);
    }
}

