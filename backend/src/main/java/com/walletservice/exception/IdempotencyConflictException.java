package com.walletservice.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends BusinessException {

    public IdempotencyConflictException(String message, HttpStatus status, String code) {
        super(message, status, code);
    }

    public static IdempotencyConflictException keyReusedWithDifferentPayload() {
        return new IdempotencyConflictException(
                "Idempotency key reused with different request payload",
                HttpStatus.UNPROCESSABLE_ENTITY,
                "IDEMPOTENCY_KEY_REUSED"
        );
    }

    public static IdempotencyConflictException requestInProgress() {
        return new IdempotencyConflictException(
                "A request with this idempotency key is already in progress",
                HttpStatus.CONFLICT,
                "REQUEST_IN_PROGRESS"
        );
    }
}
