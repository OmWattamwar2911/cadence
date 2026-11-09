package com.walletservice.exception;

import org.springframework.http.HttpStatus;

public class FraudRuleViolationException extends BusinessException {

    public FraudRuleViolationException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "FRAUD_LIMIT_EXCEEDED");
    }
}
