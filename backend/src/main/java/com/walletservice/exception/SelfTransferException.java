package com.walletservice.exception;

import org.springframework.http.HttpStatus;

public class SelfTransferException extends BusinessException {

    public SelfTransferException() {
        super("Sender and recipient cannot be the same user", HttpStatus.BAD_REQUEST, "SELF_TRANSFER_NOT_ALLOWED");
    }
}
