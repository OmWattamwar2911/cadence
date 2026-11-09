package com.walletservice.dto.response;

import java.time.Instant;

public record RegisterResponse(
        Long userId,
        String email,
        Long walletId,
        String currency,
        Instant createdAt
) {}
