package com.walletservice.service;

import com.walletservice.entity.Wallet;
import com.walletservice.exception.FraudRuleViolationException;
import com.walletservice.repository.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class FraudCheckService {

    private static final Logger log = LoggerFactory.getLogger(FraudCheckService.class);

    private final TransferRepository transferRepository;
    private final long maxAmountPerTransfer;
    private final int velocityMaxTransfers;
    private final int velocityWindowMinutes;

    public FraudCheckService(
            TransferRepository transferRepository,
            @Value("${app.fraud.max-amount-per-transfer:10000000}") long maxAmountPerTransfer,
            @Value("${app.fraud.velocity-max-transfers:5}") int velocityMaxTransfers,
            @Value("${app.fraud.velocity-window-minutes:5}") int velocityWindowMinutes) {
        this.transferRepository = transferRepository;
        this.maxAmountPerTransfer = maxAmountPerTransfer;
        this.velocityMaxTransfers = velocityMaxTransfers;
        this.velocityWindowMinutes = velocityWindowMinutes;
    }

    public void validateTransfer(Wallet senderWallet, Long amount) {
        // 1. Max amount check
        if (amount > maxAmountPerTransfer) {
            log.warn("Transfer rejected: amount {} exceeds max allowed {}", amount, maxAmountPerTransfer);
            throw new FraudRuleViolationException(
                    String.format("Transfer amount %d exceeds maximum allowed limit of %d paise", amount, maxAmountPerTransfer));
        }

        // 2. Velocity check (max N transfers in M minutes)
        Instant windowStart = Instant.now().minus(velocityWindowMinutes, ChronoUnit.MINUTES);
        long recentTransferCount = transferRepository.countTransfersSince(senderWallet.getId(), windowStart);

        if (recentTransferCount >= velocityMaxTransfers) {
            log.warn("Transfer rejected for wallet {}: {} transfers in last {} minutes (limit: {})",
                    senderWallet.getId(), recentTransferCount, velocityWindowMinutes, velocityMaxTransfers);
            throw new FraudRuleViolationException(
                    String.format("Velocity limit exceeded: maximum of %d transfers allowed within %d minutes",
                            velocityMaxTransfers, velocityWindowMinutes));
        }
    }
}
