package com.walletservice.dto.response;

import java.time.Instant;

public record WalletResponse(
        Long walletId,
        Long userId,
        Long balance,
        String currency,
        Instant updatedAt
) {}
