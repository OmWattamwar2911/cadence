package com.walletservice.dto.response;

import java.time.Instant;

public record DepositResponse(
        Long walletId,
        Long depositAmount,
        Long newBalance,
        String currency,
        String reference,
        Instant timestamp
) {}
