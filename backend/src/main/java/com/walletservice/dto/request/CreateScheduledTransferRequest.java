package com.walletservice.dto.request;

import com.walletservice.entity.ScheduleFrequency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record CreateScheduledTransferRequest(
        @NotBlank(message = "Recipient email is required")
        @Email(message = "Invalid recipient email format")
        String recipientEmail,

        @NotNull(message = "Transfer amount is required")
        @Positive(message = "Amount must be strictly greater than 0")
        Long amount,

        @NotNull(message = "Frequency is required (DAILY, WEEKLY, MONTHLY)")
        ScheduleFrequency frequency,

        Instant startDate
) {}
