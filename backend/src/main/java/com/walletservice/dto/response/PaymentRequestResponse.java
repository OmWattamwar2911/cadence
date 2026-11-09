package com.walletservice.dto.response;

import com.walletservice.entity.PaymentRequest;
import com.walletservice.entity.PaymentRequestStatus;

import java.time.Instant;

public record PaymentRequestResponse(
        Long id,
        Long requesterUserId,
        String requesterEmail,
        String payerEmail,
        Long amount,
        String formattedAmount,
        String note,
        PaymentRequestStatus status,
        Long transferId,
        Instant paidAt,
        Instant createdAt
) {
    public static PaymentRequestResponse from(PaymentRequest pr) {
        long main = pr.getAmount() / 100;
        long minor = Math.abs(pr.getAmount() % 100);
        String formatted = String.format("₹%d.%02d", main, minor);
        return new PaymentRequestResponse(
                pr.getId(),
                pr.getRequester().getId(),
                pr.getRequester().getEmail(),
                pr.getPayerEmail(),
                pr.getAmount(),
                formatted,
                pr.getNote(),
                pr.getStatus(),
                pr.getTransfer() != null ? pr.getTransfer().getId() : null,
                pr.getPaidAt(),
                pr.getCreatedAt()
        );
    }
}
