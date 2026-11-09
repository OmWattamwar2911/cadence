package com.walletservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DepositRequest(
        @NotNull(message = "Deposit amount is required")
        @Positive(message = "Deposit amount must be strictly greater than 0")
        @Max(value = 100_000_000L, message = "Deposit amount cannot exceed 100,000,000 paise (₹1,000,000)")
        Long amount
) {}
