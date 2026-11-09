package com.walletservice.service;

import com.walletservice.dto.response.PageResponse;
import com.walletservice.dto.response.TransactionResponse;
import com.walletservice.entity.EntryType;
import com.walletservice.entity.LedgerEntry;
import com.walletservice.exception.ResourceNotFoundException;
import com.walletservice.repository.LedgerEntryRepository;
import com.walletservice.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TransactionService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final WalletRepository walletRepository;

    public TransactionService(LedgerEntryRepository ledgerEntryRepository, WalletRepository walletRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getTransactions(
            Long userId,
            EntryType type,
            Instant from,
            Instant to,
            int page,
            int size) {

        if (size > 100) {
            throw new IllegalArgumentException("Page size cannot exceed 100");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be at least 1");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page index cannot be negative");
        }

        Long walletId = walletRepository.findWalletIdByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));

        Pageable pageable = PageRequest.of(page, size);
        Page<LedgerEntry> entries = ledgerEntryRepository.findTransactions(walletId, type, from, to, pageable);

        Page<TransactionResponse> mapped = entries.map(le -> new TransactionResponse(
                le.getId(),
                le.getEntryType(),
                le.getAmount(),
                le.getBalanceAfter(),
                le.getTransfer() != null ? le.getTransfer().getReference() : null,
                le.getTransfer() != null ? le.getTransfer().getId() : null,
                le.getCreatedAt()
        ));

        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public byte[] exportStatementCsv(Long userId) {
        Long walletId = walletRepository.findWalletIdByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));

        java.util.List<LedgerEntry> entries = ledgerEntryRepository.findAllByWalletIdOrderByIdAsc(walletId);

        StringBuilder csv = new StringBuilder();
        csv.append("CADENCE OFFICIAL FINANCIAL STATEMENT\n");
        csv.append("Wallet ID: ").append(walletId).append("\n");
        csv.append("Export Date: ").append(Instant.now()).append("\n\n");
        csv.append("Entry ID,Timestamp,Type,Amount (INR),Balance After (INR),Transfer Reference,Cryptographic Hash\n");

        for (LedgerEntry entry : entries) {
            long amtUnits = entry.getAmount() / 100;
            long amtMinor = Math.abs(entry.getAmount() % 100);
            String formattedAmt = String.format("₹%d.%02d", amtUnits, amtMinor);

            long balUnits = entry.getBalanceAfter() / 100;
            long balMinor = Math.abs(entry.getBalanceAfter() % 100);
            String formattedBal = String.format("₹%d.%02d", balUnits, balMinor);

            String ref = (entry.getTransfer() != null && entry.getTransfer().getReference() != null)
                    ? entry.getTransfer().getReference() : "N/A";
            String hash = entry.getEntryHash() != null ? entry.getEntryHash() : "N/A";

            csv.append(entry.getId()).append(",")
               .append(entry.getCreatedAt()).append(",")
               .append(entry.getEntryType()).append(",")
               .append("\"").append(formattedAmt).append("\",")
               .append("\"").append(formattedBal).append("\",")
               .append(ref).append(",")
               .append(hash).append("\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
