package com.walletservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walletservice.dto.request.DepositRequest;
import com.walletservice.dto.response.DepositResponse;
import com.walletservice.dto.response.WalletResponse;
import com.walletservice.entity.IdempotencyKey;
import com.walletservice.entity.Wallet;
import com.walletservice.exception.ResourceNotFoundException;
import com.walletservice.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final TransactionalDepositService transactionalDepositService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public WalletService(
            WalletRepository walletRepository,
            TransactionalDepositService transactionalDepositService,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper) {
        this.walletRepository = walletRepository;
        this.transactionalDepositService = transactionalDepositService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user id: " + userId));
        return WalletResponse.of(
                wallet.getId(),
                wallet.getUser().getId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getUpdatedAt()
        );
    }

    // Orchestrator method is deliberately NOT @Transactional so that a caught
    // DataIntegrityViolationException does not poison the JPA session.
    public DepositResponse deposit(Long userId, String idemKey, DepositRequest request) {
        validateIdempotencyKey(idemKey);
        String requestHash = idempotencyService.computeHash("DEPOSIT:" + userId + ":" + request.amount());

        try {
            return transactionalDepositService.executeDeposit(userId, idemKey, requestHash, request.amount());
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate idempotency key detected for userId={}, key={}. Resolving existing record.", userId, idemKey);
            IdempotencyKey existingKey = idempotencyService.resolveDuplicateKey(userId, idemKey, requestHash);

            try {
                return objectMapper.readValue(existingKey.getResponseBody(), DepositResponse.class);
            } catch (JsonProcessingException ex) {
                log.error("Failed to deserialize cached response for key: {}", idemKey, ex);
                throw new IllegalStateException("Stored response could not be parsed", ex);
            }
        }
    }

    private void validateIdempotencyKey(String idemKey) {
        if (idemKey == null || idemKey.trim().isEmpty() || idemKey.length() > 64) {
            throw new IllegalArgumentException("Idempotency-Key header is required and must be between 1 and 64 characters");
        }
    }
}
