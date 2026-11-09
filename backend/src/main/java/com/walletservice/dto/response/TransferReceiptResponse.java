package com.walletservice.dto.response;

import java.time.Instant;

public record TransferReceiptResponse(
        String transferReference,
        Long transferId,
        Long fromWalletId,
        Long toWalletId,
        Long amount,
        String formattedAmount,
        String currency,
        String status,
        String cryptographicHash,
        String previousHash,
        Instant completedAt,
        String certificationStamp
) {}
