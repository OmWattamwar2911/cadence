package com.walletservice.dto.response;

import java.time.Instant;

public record LedgerAuditResponse(
        Long walletId,
        boolean isTamperFree,
        int totalVerifiedEntries,
        String rootHash,
        String genesisHash,
        Instant auditedAt,
        String statusDescription
) {}
