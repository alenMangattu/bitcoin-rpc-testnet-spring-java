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

@RestController
@Validated
public class MainRestController {

    private final AccountInfo accountInfo;
    private final SendBitcoinService sendBitcoinService;

    public MainRestController(AccountInfo accountInfo, SendBitcoinService sendBitcoinService) {
        this.accountInfo = accountInfo;
        this.sendBitcoinService = sendBitcoinService;
    }

    @GetMapping({"/account", "/account/"})
    public ResponseEntity<Map<String, Object>> getAccountInfo() {
        return this.accountInfo.execute(null);
    }

    @GetMapping("/account/{userId}")
    public ResponseEntity<Map<String, Object>> getAccountInfoById(
            @PathVariable @Positive(message = "User ID must be a positive number") Long userId) {
        return this.accountInfo.executeWithId(userId);
    }

    @PostMapping("/send")
    public ResponseEntity<TransactionDTO> sendBitcoin(@Valid @RequestBody SendRequestDTO request) {
        return this.sendBitcoinService.execute(request);
    }
}
