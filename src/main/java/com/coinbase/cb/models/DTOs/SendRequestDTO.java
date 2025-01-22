package com.coinbase.cb.models.DTOs;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SendRequestDTO {
    @NotNull(message = "Recipient identifier is required")
    private String recipient; // Can be either userId or wallet address

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Integer amount;

    private String message; // Optional message
} 