package com.walletservice.exception;

import org.springframework.http.HttpStatus;

public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(Long currentBalance, Long requestedAmount) {
        super(String.format("Insufficient funds: current balance is %d paise, transfer requires %d paise",
                currentBalance, requestedAmount),
                HttpStatus.UNPROCESSABLE_ENTITY,
                "INSUFFICIENT_FUNDS");
    }
}
