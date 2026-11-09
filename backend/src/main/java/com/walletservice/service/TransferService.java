package com.walletservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.walletservice.dto.response.TransferResponse;
import com.walletservice.entity.*;
import com.walletservice.exception.FraudRuleViolationException;
import com.walletservice.exception.InsufficientBalanceException;
import com.walletservice.exception.ResourceNotFoundException;
import com.walletservice.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final FraudCheckService fraudCheckService;
    private final TransferExecutionHook transferExecutionHook;
    private final ObjectMapper objectMapper;

    public TransferService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            TransferRepository transferRepository,
            LedgerEntryRepository ledgerEntryRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            FraudCheckService fraudCheckService,
            TransferExecutionHook transferExecutionHook,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.fraudCheckService = fraudCheckService;
        this.transferExecutionHook = transferExecutionHook;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransferResponse executeTransfer(
            Long senderUserId,
            Long receiverUserId,
            String idemKey,
            String requestHash,
            Long amount,
            String reference) {

        User senderUser = userRepository.findById(senderUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender user not found: " + senderUserId));

        // 1. Insert idempotency row with IN_PROGRESS status inside the transaction (flushes to trigger unique constraint collision)
        IdempotencyKey idempotencyKey = new IdempotencyKey(senderUser, idemKey, requestHash, IdempotencyStatus.IN_PROGRESS);
        idempotencyKey = idempotencyKeyRepository.saveAndFlush(idempotencyKey);

        // 2. Identify sender and receiver wallet IDs without loading entities into persistence context
        Long senderWalletId = walletRepository.findWalletIdByUserId(senderUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found for user: " + senderUserId));
        Long receiverWalletId = walletRepository.findWalletIdByUserId(receiverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver wallet not found for user: " + receiverUserId));

        // 3. Acquire PESSIMISTIC_WRITE locks ordered by wallet ID ascending to eliminate deadlocks
        Long firstId = Math.min(senderWalletId, receiverWalletId);
        Long secondId = Math.max(senderWalletId, receiverWalletId);

        Wallet firstLocked = walletRepository.findByIdWithLock(firstId)
                .orElseThrow(() -> new ResourceNotFoundException("Failed to acquire lock for wallet: " + firstId));
        Wallet secondLocked = walletRepository.findByIdWithLock(secondId)
                .orElseThrow(() -> new ResourceNotFoundException("Failed to acquire lock for wallet: " + secondId));

        Wallet lockedSender = firstLocked.getId().equals(senderWalletId) ? firstLocked : secondLocked;
        Wallet lockedReceiver = firstLocked.getId().equals(receiverWalletId) ? firstLocked : secondLocked;

        // 4. Check sufficient balance
        if (lockedSender.getBalance() < amount) {
            log.warn("Transfer failed: insufficient funds in wallet {}. Balance={}, Required={}",
                    lockedSender.getId(), lockedSender.getBalance(), amount);
            throw new InsufficientBalanceException(lockedSender.getBalance(), amount);
        }

        // 5. Apply fraud rules
        try {
            fraudCheckService.validateTransfer(lockedSender, amount);
        } catch (FraudRuleViolationException ex) {
            recordRejectedTransfer(lockedSender, lockedReceiver, amount, reference);
            throw ex;
        }

        // 6. Safe balance updates preventing overflow
        long senderBalanceAfter = Math.subtractExact(lockedSender.getBalance(), amount);
        long receiverBalanceAfter = Math.addExact(lockedReceiver.getBalance(), amount);

        lockedSender.setBalance(senderBalanceAfter);
        lockedReceiver.setBalance(receiverBalanceAfter);
        walletRepository.save(lockedSender);
        walletRepository.save(lockedReceiver);

        // 7. Test hook for simulating unexpected failure mid-transfer
        transferExecutionHook.triggerOnAfterDebit(lockedSender, lockedReceiver);

        // 8. Insert Transfer record
        Transfer transfer = new Transfer(lockedSender, lockedReceiver, amount, TransferStatus.COMPLETED, reference);
        transfer = transferRepository.save(transfer);

        // 9. Insert two Ledger entries (DEBIT for sender, CREDIT for receiver)
        LedgerEntry debitEntry = new LedgerEntry(transfer, lockedSender, EntryType.DEBIT, amount, senderBalanceAfter);
        LedgerEntry creditEntry = new LedgerEntry(transfer, lockedReceiver, EntryType.CREDIT, amount, receiverBalanceAfter);
        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        // 10. Mark idempotency key as COMPLETED with serialized response
        TransferResponse response = new TransferResponse(
                transfer.getId(),
                lockedSender.getId(),
                lockedReceiver.getId(),
                amount,
                lockedSender.getCurrency(),
                TransferStatus.COMPLETED,
                reference,
                senderBalanceAfter,
                transfer.getCreatedAt()
        );

        try {
            idempotencyKey.setStatus(IdempotencyStatus.COMPLETED);
            idempotencyKey.setResponseStatus(200);
            idempotencyKey.setResponseBody(objectMapper.writeValueAsString(response));
            idempotencyKey.setTransfer(transfer);
            idempotencyKeyRepository.save(idempotencyKey);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize transfer response for idempotency key: {}", idemKey, e);
            throw new IllegalStateException("Failed to store idempotency response", e);
        }

        log.info("Transfer completed successfully: id={}, from={}, to={}, amount={}",
                transfer.getId(), lockedSender.getId(), lockedReceiver.getId(), amount);

        return response;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordRejectedTransfer(Wallet sender, Wallet receiver, Long amount, String reference) {
        Transfer rejectedTransfer = new Transfer(sender, receiver, amount, TransferStatus.REJECTED, reference);
        transferRepository.save(rejectedTransfer);
        log.info("Recorded REJECTED transfer: from={}, to={}, amount={}", sender.getId(), receiver.getId(), amount);
    }
}
