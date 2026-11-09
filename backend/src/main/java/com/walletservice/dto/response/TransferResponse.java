package com.walletservice.dto.response;

import com.walletservice.entity.TransferStatus;

import java.time.Instant;

public record TransferResponse(
        Long transferId,
        Long fromWalletId,
        Long toWalletId,
        Long amount,
        String currency,
        TransferStatus status,
        String reference,
        Long senderBalanceAfter,
        Instant createdAt
) {}
