package com.walletservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePaymentRequest(
        @NotBlank(message = "Payer email is required")
        @Email(message = "Invalid payer email format")
        String payerEmail,

        @NotNull(message = "Requested amount is required")
        @Positive(message = "Amount must be strictly greater than 0")
        Long amount,

        @Size(max = 255, message = "Note cannot exceed 255 characters")
        String note
) {}
