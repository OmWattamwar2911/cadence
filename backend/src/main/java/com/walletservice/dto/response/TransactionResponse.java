package com.walletservice.dto.response;

import com.walletservice.entity.EntryType;

import java.time.Instant;

public record TransactionResponse(
        Long id,
        EntryType type,
        Long amount,
        Long balanceAfter,
        String reference,
        Long transferId,
        Instant createdAt
) {}
