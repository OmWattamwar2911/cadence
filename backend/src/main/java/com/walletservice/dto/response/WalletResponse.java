package com.walletservice.dto.response;

import java.time.Instant;

public record WalletResponse(
        Long walletId,
        Long userId,
        Long balance,
        String formattedBalance,
        String currency,
        Instant updatedAt
) {
    public static WalletResponse of(Long walletId, Long userId, Long balance, String currency, Instant updatedAt) {
        long bal = (balance != null ? balance : 0L);
        long mainUnits = bal / 100;
        long minorUnits = Math.abs(bal % 100);
        String formatted = String.format("₹%d.%02d", mainUnits, minorUnits);
        return new WalletResponse(walletId, userId, balance, formatted, currency, updatedAt);
    }
}
