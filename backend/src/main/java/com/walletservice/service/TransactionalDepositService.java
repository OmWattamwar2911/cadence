package com.walletservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walletservice.dto.response.DepositResponse;
import com.walletservice.entity.*;
import com.walletservice.exception.ResourceNotFoundException;
import com.walletservice.repository.IdempotencyKeyRepository;
import com.walletservice.repository.LedgerEntryRepository;
import com.walletservice.repository.UserRepository;
import com.walletservice.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class TransactionalDepositService {

    private static final Logger log = LoggerFactory.getLogger(TransactionalDepositService.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public TransactionalDepositService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            LedgerEntryRepository ledgerEntryRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DepositResponse executeDeposit(Long userId, String idemKey, String requestHash, Long amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // 1. Insert idempotency row with IN_PROGRESS status (triggers unique constraint on collision)
        IdempotencyKey idempotencyKey = new IdempotencyKey(user, idemKey, requestHash, IdempotencyStatus.IN_PROGRESS);
        idempotencyKey = idempotencyKeyRepository.saveAndFlush(idempotencyKey);

        // 2. Lock wallet with PESSIMISTIC_WRITE
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));

        Wallet lockedWallet = walletRepository.findByIdWithLock(wallet.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Failed to acquire lock for wallet: " + wallet.getId()));

        // 3. Prevent overflow and update balance
        long newBalance = Math.addExact(lockedWallet.getBalance(), amount);
        lockedWallet.setBalance(newBalance);
        walletRepository.save(lockedWallet);

        // 4. Create ledger DEPOSIT entry
        LedgerEntry ledgerEntry = new LedgerEntry(null, lockedWallet, EntryType.DEPOSIT, amount, newBalance);
        ledgerEntryRepository.save(ledgerEntry);

        String reference = "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        DepositResponse response = new DepositResponse(
                lockedWallet.getId(),
                amount,
                newBalance,
                lockedWallet.getCurrency(),
                reference,
                Instant.now()
        );

        // 5. Complete idempotency key
        try {
            idempotencyKey.setStatus(IdempotencyStatus.COMPLETED);
            idempotencyKey.setResponseStatus(200);
            idempotencyKey.setResponseBody(objectMapper.writeValueAsString(response));
            idempotencyKeyRepository.save(idempotencyKey);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize deposit response for idempotency key: {}", idemKey, e);
            throw new IllegalStateException("Failed to store idempotency response", e);
        }

        log.info("Deposit completed: walletId={}, amount={}, newBalance={}, ref={}",
                lockedWallet.getId(), amount, newBalance, reference);

        return response;
    }
}
