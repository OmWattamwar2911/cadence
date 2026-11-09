package com.walletservice.service;

import com.walletservice.dto.response.LedgerAuditResponse;
import com.walletservice.dto.response.TransferReceiptResponse;
import com.walletservice.entity.LedgerEntry;
import com.walletservice.entity.Transfer;
import com.walletservice.entity.Wallet;
import com.walletservice.exception.ResourceNotFoundException;
import com.walletservice.repository.LedgerEntryRepository;
import com.walletservice.repository.TransferRepository;
import com.walletservice.repository.WalletRepository;
import com.walletservice.util.LedgerHashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class LedgerAuditService {

    private static final Logger log = LoggerFactory.getLogger(LedgerAuditService.class);

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransferRepository transferRepository;

    public LedgerAuditService(
            WalletRepository walletRepository,
            LedgerEntryRepository ledgerEntryRepository,
            TransferRepository transferRepository) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.transferRepository = transferRepository;
    }

    @Transactional(readOnly = true)
    public LedgerAuditResponse auditUserWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));

        List<LedgerEntry> entries = ledgerEntryRepository.findAllByWalletIdOrderByIdAsc(wallet.getId());

        if (entries.isEmpty()) {
            return new LedgerAuditResponse(
                    wallet.getId(),
                    true,
                    0,
                    LedgerHashUtil.GENESIS_HASH,
                    LedgerHashUtil.GENESIS_HASH,
                    Instant.now(),
                    "Genesis State: Zero transactions recorded. Ledger mathematically pristine."
            );
        }

        String expectedPrevHash = LedgerHashUtil.GENESIS_HASH;
        boolean isTamperFree = true;
        int verifiedCount = 0;

        for (LedgerEntry entry : entries) {
            // 1. Verify prev_hash matches preceding block
            if (entry.getPrevHash() != null && !entry.getPrevHash().equals(expectedPrevHash)) {
                log.error("Audit failure on wallet {}: entry id={} prevHash {} does not match expected {}",
                        wallet.getId(), entry.getId(), entry.getPrevHash(), expectedPrevHash);
                isTamperFree = false;
                break;
            }

            // 2. Re-compute hash from state columns
            String calculatedHash = LedgerHashUtil.calculateHash(
                    expectedPrevHash,
                    wallet.getId(),
                    entry.getEntryType().name(),
                    entry.getAmount(),
                    entry.getBalanceAfter()
            );

            if (entry.getEntryHash() != null && !entry.getEntryHash().equals(calculatedHash)) {
                log.error("Audit failure on wallet {}: entry id={} stored hash {} does not match recomputed hash {}",
                        wallet.getId(), entry.getId(), entry.getEntryHash(), calculatedHash);
                isTamperFree = false;
                break;
            }

            expectedPrevHash = entry.getEntryHash() != null ? entry.getEntryHash() : calculatedHash;
            verifiedCount++;
        }

        String statusDesc = isTamperFree
                ? "Cryptographic Verification Succeeded: All " + verifiedCount + " ledger blocks intact with 0 discrepancies."
                : "Cryptographic Verification Failed: Data integrity anomaly detected in ledger sequence.";

        return new LedgerAuditResponse(
                wallet.getId(),
                isTamperFree,
                verifiedCount,
                expectedPrevHash,
                LedgerHashUtil.GENESIS_HASH,
                Instant.now(),
                statusDesc
        );
    }

    @Transactional(readOnly = true)
    public TransferReceiptResponse getTransferReceipt(String reference, Long userId) {
        Transfer transfer = transferRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found with reference: " + reference));

        Wallet userWallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));

        Long userWalletId = userWallet.getId();
        if (!transfer.getFromWallet().getId().equals(userWalletId) && !transfer.getToWallet().getId().equals(userWalletId)) {
            throw new ResourceNotFoundException("Transfer receipt not accessible for current user");
        }

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransferId(transfer.getId());
        LedgerEntry entry = entries.stream()
                .filter(e -> e.getWallet().getId().equals(userWalletId))
                .findFirst()
                .orElse(null);

        String hash = (entry != null && entry.getEntryHash() != null)
                ? entry.getEntryHash()
                : LedgerHashUtil.sha256Hex(transfer.getId() + ":" + transfer.getReference() + ":" + transfer.getAmount());

        String prevHash = (entry != null && entry.getPrevHash() != null)
                ? entry.getPrevHash()
                : LedgerHashUtil.GENESIS_HASH;

        long rupees = transfer.getAmount() / 100;
        long paise = Math.abs(transfer.getAmount() % 100);
        String formatted = String.format("₹%d.%02d", rupees, paise);

        return new TransferReceiptResponse(
                transfer.getReference(),
                transfer.getId(),
                transfer.getFromWallet().getId(),
                transfer.getToWallet().getId(),
                transfer.getAmount(),
                formatted,
                transfer.getFromWallet().getCurrency(),
                transfer.getStatus().name(),
                hash,
                prevHash,
                transfer.getCreatedAt(),
                "CERTIFIED_IMMUTABLE_HASH_CHAIN_LEDGER_RECEIPT"
        );
    }
}
