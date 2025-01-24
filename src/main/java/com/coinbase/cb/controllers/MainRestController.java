package com.coinbase.cb.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;

import com.coinbase.cb.controllers.services.AccountInfo;
import com.coinbase.cb.controllers.services.SendBitcoinService;
import com.coinbase.cb.models.DTOs.SendRequestDTO;
import com.coinbase.cb.models.DTOs.TransactionDTO;
import java.util.Map;

/**
 * Main REST controller for handling account and transaction operations.
 * All endpoints require JWT authentication unless explicitly marked as public.
 */
@RestController
@Validated
public class MainRestController {

    private final AccountInfo accountInfo;
    private final SendBitcoinService sendBitcoinService;

    public MainRestController(AccountInfo accountInfo, SendBitcoinService sendBitcoinService) {
        this.accountInfo = accountInfo;
        this.sendBitcoinService = sendBitcoinService;
    }

    /**
     * Get account information for the authenticated user.
     * 
     * Endpoint: GET /account or /account/
     * Authentication: Required (JWT)
     * Rate Limit: 8 requests per minute
     * 
     * @return Account information including:
     *         - User details (name, email)
     *         - Wallet information (address, balance)
     *         - Transaction history
     */
    @GetMapping({"/account", "/account/"})
    public ResponseEntity<Map<String, Object>> getAccountInfo() {
        return this.accountInfo.execute(null);
    }

    /**
     * Get account information for a specific user by ID.
     * 
     * Endpoint: GET /account/{userId}
     * Authentication: Required (JWT)
     * Rate Limit: 8 requests per minute
     * 
     * @param userId The ID of the user to fetch information for (must be positive)
     * @return Account information for the specified user
     * @throws IllegalArgumentException if userId is not positive
     */
    @GetMapping("/account/{userId}")
    public ResponseEntity<Map<String, Object>> getAccountInfoById(
            @PathVariable @Positive(message = "User ID must be a positive number") Long userId) {
        return this.accountInfo.executeWithId(userId);
    }

    /**
     * Send Bitcoin to another user or wallet address.
     * 
     * Endpoint: POST /send
     * Authentication: Required (JWT)
     * Rate Limit: 2 requests per minute
     * 
     * Request Body:
     * {
     *   "recipient": "string",  // User ID or wallet address
     *   "amount": number,       // Amount in BTC (must be positive)
     *   "message": "string"     // Optional transaction message
     * }
     * 
     * @param request The send request containing recipient, amount, and optional message
     * @return Transaction details including blockchain transaction ID
     * @throws CustomErrorResponse for various error conditions:
     *         - Insufficient balance
     *         - Invalid recipient
     *         - Transaction failure
     */
    @PostMapping("/send")
    public ResponseEntity<TransactionDTO> sendBitcoin(@Valid @RequestBody SendRequestDTO request) {
        return this.sendBitcoinService.execute(request);
    }
}

