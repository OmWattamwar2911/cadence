package com.walletservice.dto.response;

public record AuthResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds
) {}
