package com.walletservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walletservice.dto.request.TransferRequest;
import com.walletservice.dto.response.TransferResponse;
import com.walletservice.entity.IdempotencyKey;
import com.walletservice.entity.User;
import com.walletservice.exception.FraudRuleViolationException;
import com.walletservice.exception.ResourceNotFoundException;
import com.walletservice.exception.SelfTransferException;
import com.walletservice.repository.UserRepository;
import com.walletservice.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class TransferOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TransferOrchestrator.class);
    private static final int MAX_LOCK_RETRIES = 3;

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransferService transferService;
    private final TransferRejectionService transferRejectionService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public TransferOrchestrator(
            UserRepository userRepository,
            WalletRepository walletRepository,
            TransferService transferService,
            TransferRejectionService transferRejectionService,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transferService = transferService;
        this.transferRejectionService = transferRejectionService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    // Orchestrator method is deliberately NOT @Transactional so duplicate key collisions
    // on idempotency_keys do not corrupt the Hibernate PersistenceContext.
    public TransferResponse transfer(Long senderUserId, String idemKey, TransferRequest request) {
        validateIdempotencyKey(idemKey);

        String normalizedRecipientEmail = request.toEmail().trim().toLowerCase();
        User receiver = userRepository.findByEmail(normalizedRecipientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found with email: " + normalizedRecipientEmail));

        if (senderUserId.equals(receiver.getId())) {
            throw new SelfTransferException();
        }

        String requestHash = idempotencyService.computeHash(
                senderUserId + ":" + receiver.getId() + ":" + request.amount());

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return transferService.executeTransfer(
                        senderUserId,
                        receiver.getId(),
                        idemKey,
                        requestHash,
                        request.amount(),
                        request.reference()
                );
            } catch (FraudRuleViolationException e) {
                // Fraud rule was violated: the transfer transaction has rolled back.
                // Record the REJECTED transfer audit trail in an autonomous committed transaction.
                Long senderWalletId = walletRepository.findWalletIdByUserId(senderUserId).orElse(null);
                Long receiverWalletId = walletRepository.findWalletIdByUserId(receiver.getId()).orElse(null);
                if (senderWalletId != null && receiverWalletId != null) {
                    transferRejectionService.recordRejectedTransfer(
                            senderWalletId, receiverWalletId, request.amount(), request.reference());
                }
                throw e;
            } catch (DataIntegrityViolationException e) {
                log.info("Duplicate idempotency key detected for transfer: userId={}, key={}", senderUserId, idemKey);
                IdempotencyKey existingKey = idempotencyService.resolveDuplicateKey(senderUserId, idemKey, requestHash);

                try {
                    return objectMapper.readValue(existingKey.getResponseBody(), TransferResponse.class);
                } catch (JsonProcessingException ex) {
                    log.error("Failed to deserialize cached transfer response for key: {}", idemKey, ex);
                    throw new IllegalStateException("Stored transfer response could not be parsed", ex);
                }
            } catch (ConcurrencyFailureException e) {
                if (attempt >= MAX_LOCK_RETRIES) {
                    log.error("Lock acquisition retry limit exceeded ({}/{}) for transfer key: {}",
                            attempt, MAX_LOCK_RETRIES, idemKey, e);
                    throw e;
                }
                long backoffMs = (50L * attempt) + random.nextInt(30);
                log.warn("Database lock conflict on attempt {}/{}, retrying in {} ms...", attempt, MAX_LOCK_RETRIES, backoffMs);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Transfer thread interrupted during retry backoff", ie);
                }
            }
        }
    }

    private void validateIdempotencyKey(String idemKey) {
        if (idemKey == null || idemKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
        if (idemKey.length() > 64) {
            throw new IllegalArgumentException("Idempotency-Key header must not exceed 64 characters");
        }
    }
}
