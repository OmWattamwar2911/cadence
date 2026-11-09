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
        double mainUnits = (balance != null ? balance : 0L) / 100.0;
        String formatted = String.format("₹%.2f", mainUnits);
        return new WalletResponse(walletId, userId, balance, formatted, currency, updatedAt);
    }
}
