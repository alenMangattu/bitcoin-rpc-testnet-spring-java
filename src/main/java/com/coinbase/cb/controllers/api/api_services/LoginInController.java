package com.coinbase.cb.controllers.api.api_services;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.coinbase.cb.controllers.Query;
import com.coinbase.cb.models.DTOs.UserDTO;
import com.coinbase.cb.models.DbTables.User;
import com.coinbase.cb.configuration.security.JwtService;
import com.coinbase.cb.repository.UserRespository;
import com.coinbase.cb.util.validators.LoginInputValidator;
import com.coinbase.cb.error.CustomErrorResponse;
import com.coinbase.cb.error.ErrorMessages;
import com.coinbase.cb.util.Base64Util;
import com.coinbase.cb.util.HashUtil;

@Service
public class LoginInController implements Query<User, UserDTO> {

    private final JwtService jwtService;
    private final UserRespository userRepository;

    public LoginInController(JwtService jwtService, UserRespository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public ResponseEntity<UserDTO> execute(User loginRequest) {
        // Validate input format
        LoginInputValidator.validate(loginRequest);

        // Find user and authenticate
        User user = userRepository.findByEmail(loginRequest.getEmail())
            .orElseThrow(() -> new CustomErrorResponse(ErrorMessages.INVALID_CREDENTIALS.getMessage(), HttpStatus.NOT_FOUND));

        // Verify password
        String decodedPassword = Base64Util.decode(loginRequest.getPassword());
        String hashedPassword = HashUtil.getSHA256(decodedPassword);

        if (!user.getPassword().equals(hashedPassword)) {
            throw new CustomErrorResponse(ErrorMessages.INVALID_CREDENTIALS.getMessage(), HttpStatus.UNAUTHORIZED);
        }

        // Generate JWT token
        String token = jwtService.generateToken(user);
        
        // Create response headers with token
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(new UserDTO(
                    user.getFirstName(), 
                    user.getLastName(), 
                    user.getEmail(), 
                    user.getWallet().getBalance()
                ));
    }
}
