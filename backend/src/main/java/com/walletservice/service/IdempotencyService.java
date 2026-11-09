package com.walletservice.service;

import com.walletservice.entity.IdempotencyKey;
import com.walletservice.entity.IdempotencyStatus;
import com.walletservice.exception.IdempotencyConflictException;
import com.walletservice.repository.IdempotencyKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }

    public String computeHash(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyKey> findExistingKey(Long userId, String idemKey) {
        return idempotencyKeyRepository.findByUserIdAndIdemKey(userId, idemKey);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public IdempotencyKey resolveDuplicateKey(Long userId, String idemKey, String expectedHash) {
        IdempotencyKey existing = idempotencyKeyRepository.findByUserIdAndIdemKey(userId, idemKey)
                .orElseThrow(() -> new IllegalStateException("Duplicate key violation occurred but record was not found"));

        if (!existing.getRequestHash().equals(expectedHash)) {
            throw IdempotencyConflictException.keyReusedWithDifferentPayload();
        }

        if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            throw IdempotencyConflictException.requestInProgress();
        }

        return existing;
    }
}
