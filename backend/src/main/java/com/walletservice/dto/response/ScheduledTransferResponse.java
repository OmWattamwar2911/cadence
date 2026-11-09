package com.walletservice.dto.response;

import com.walletservice.entity.ScheduleFrequency;
import com.walletservice.entity.ScheduleStatus;
import com.walletservice.entity.ScheduledTransfer;

import java.time.Instant;

public record ScheduledTransferResponse(
        Long id,
        String recipientEmail,
        Long amount,
        String formattedAmount,
        ScheduleFrequency frequency,
        Instant nextRunAt,
        Instant lastRunAt,
        ScheduleStatus status,
        Instant createdAt
) {
    public static ScheduledTransferResponse from(ScheduledTransfer st) {
        long main = st.getAmount() / 100;
        long minor = Math.abs(st.getAmount() % 100);
        String formatted = String.format("₹%d.%02d", main, minor);
        return new ScheduledTransferResponse(
                st.getId(),
                st.getRecipientEmail(),
                st.getAmount(),
                formatted,
                st.getFrequency(),
                st.getNextRunAt(),
                st.getLastRunAt(),
                st.getStatus(),
                st.getCreatedAt()
        );
    }
}
