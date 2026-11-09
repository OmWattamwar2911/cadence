package com.walletservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TransferRequest(
        @NotBlank(message = "Recipient email is required")
        @Email(message = "Invalid recipient email format")
        String toEmail,

        @NotNull(message = "Transfer amount is required")
        @Positive(message = "Transfer amount must be strictly greater than 0")
        Long amount,

        @Size(max = 255, message = "Reference cannot exceed 255 characters")
        String reference
) {}
