package com.coinbase.cb.models.DTOs;

import java.util.UUID;

public record TransactionDTO(UUID txid, Long from, Long to, Integer amount, String comment) {
}
