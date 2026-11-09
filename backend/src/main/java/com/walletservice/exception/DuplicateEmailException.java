package com.walletservice.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException(String email) {
        super("User already exists with email: " + email, HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS");
    }
}
