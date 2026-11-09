package com.walletservice.service;

import com.walletservice.dto.request.CreatePaymentRequest;
import com.walletservice.dto.request.TransferRequest;
import com.walletservice.dto.response.PaymentRequestResponse;
import com.walletservice.dto.response.TransferResponse;
import com.walletservice.entity.*;
import com.walletservice.exception.ResourceNotFoundException;
import com.walletservice.exception.SelfTransferException;
import com.walletservice.repository.PaymentRequestRepository;
import com.walletservice.repository.TransferRepository;
import com.walletservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class PaymentRequestService {

    private static final Logger log = LoggerFactory.getLogger(PaymentRequestService.class);

    private final PaymentRequestRepository paymentRequestRepository;
    private final UserRepository userRepository;
    private final TransferRepository transferRepository;
    private final TransferOrchestrator transferOrchestrator;
    private final SseNotificationService sseNotificationService;

    public PaymentRequestService(
            PaymentRequestRepository paymentRequestRepository,
            UserRepository userRepository,
            TransferRepository transferRepository,
            TransferOrchestrator transferOrchestrator,
            SseNotificationService sseNotificationService) {
        this.paymentRequestRepository = paymentRequestRepository;
        this.userRepository = userRepository;
        this.transferRepository = transferRepository;
        this.transferOrchestrator = transferOrchestrator;
        this.sseNotificationService = sseNotificationService;
    }

    @Transactional
    public PaymentRequestResponse createRequest(Long requesterUserId, CreatePaymentRequest request) {
        User requester = userRepository.findById(requesterUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requesterUserId));

        String normalizedPayerEmail = request.payerEmail().trim().toLowerCase();
        if (requester.getEmail().equalsIgnoreCase(normalizedPayerEmail)) {
            throw new SelfTransferException();
        }

        PaymentRequest paymentRequest = new PaymentRequest(
                requester,
                normalizedPayerEmail,
                request.amount(),
                request.note() != null ? request.note().trim() : null
        );

        PaymentRequest saved = paymentRequestRepository.save(paymentRequest);
        log.info("Created payment request id={} from user {} to payer {}", saved.getId(), requesterUserId, normalizedPayerEmail);

        // If payer is already a registered user, push real-time SSE notification
        userRepository.findByEmail(normalizedPayerEmail).ifPresent(payer -> {
            try {
                sseNotificationService.sendToUser(payer.getId(), "PAYMENT_REQUEST_RECEIVED", Map.of(
                        "type", "PAYMENT_REQUEST_RECEIVED",
                        "requestId", saved.getId(),
                        "requesterEmail", requester.getEmail(),
                        "amount", saved.getAmount(),
                        "note", saved.getNote() != null ? saved.getNote() : "",
                        "timestamp", saved.getCreatedAt()
                ));
            } catch (Exception ex) {
                log.debug("Failed to push SSE payment request notification: {}", ex.getMessage());
            }
        });

        return PaymentRequestResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentRequestResponse> getIncomingPendingRequests(String userEmail) {
        return paymentRequestRepository.findByPayerEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(userEmail.trim().toLowerCase(), PaymentRequestStatus.PENDING)
                .stream()
                .map(PaymentRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentRequestResponse> getOutgoingRequests(Long requesterUserId) {
        return paymentRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requesterUserId)
                .stream()
                .map(PaymentRequestResponse::from)
                .toList();
    }

    @Transactional
    public PaymentRequestResponse payRequest(Long payerUserId, Long requestId, String idempotencyKey) {
        User payer = userRepository.findById(payerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Payer user not found: " + payerUserId));

        PaymentRequest paymentRequest = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment request not found: " + requestId));

        if (!paymentRequest.getPayerEmail().equalsIgnoreCase(payer.getEmail())) {
            throw new ResourceNotFoundException("Payment request is not directed to the authenticated user");
        }

        if (paymentRequest.getStatus() != PaymentRequestStatus.PENDING) {
            throw new IllegalStateException("Payment request is not pending (current status: " + paymentRequest.getStatus() + ")");
        }

        // Execute transfer through battle-tested TransferOrchestrator
        String reference = "REQ-PAY-" + paymentRequest.getId();
        TransferResponse transferResponse = transferOrchestrator.transfer(
                payerUserId,
                idempotencyKey,
                new TransferRequest(paymentRequest.getRequester().getEmail(), paymentRequest.getAmount(), reference)
        );

        Transfer transfer = transferRepository.findById(transferResponse.transferId())
                .orElse(null);

        paymentRequest.setStatus(PaymentRequestStatus.PAID);
        paymentRequest.setTransfer(transfer);
        paymentRequest.setPaidAt(Instant.now());
        PaymentRequest updated = paymentRequestRepository.save(paymentRequest);

        log.info("Payment request id={} marked as PAID via transfer id={}", requestId, transferResponse.transferId());

        // Notify requester via SSE that their request was paid!
        try {
            sseNotificationService.sendToUser(paymentRequest.getRequester().getId(), "PAYMENT_REQUEST_PAID", Map.of(
                    "type", "PAYMENT_REQUEST_PAID",
                    "requestId", requestId,
                    "payerEmail", payer.getEmail(),
                    "amount", paymentRequest.getAmount(),
                    "timestamp", Instant.now()
            ));
        } catch (Exception ex) {
            log.debug("Failed to push SSE payment request paid notification: {}", ex.getMessage());
        }

        return PaymentRequestResponse.from(updated);
    }

    @Transactional
    public PaymentRequestResponse declineRequest(Long payerUserId, Long requestId) {
        User payer = userRepository.findById(payerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Payer user not found: " + payerUserId));

        PaymentRequest paymentRequest = paymentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment request not found: " + requestId));

        if (!paymentRequest.getPayerEmail().equalsIgnoreCase(payer.getEmail())) {
            throw new ResourceNotFoundException("Payment request is not directed to the authenticated user");
        }

        if (paymentRequest.getStatus() != PaymentRequestStatus.PENDING) {
            throw new IllegalStateException("Payment request is not pending");
        }

        paymentRequest.setStatus(PaymentRequestStatus.DECLINED);
        PaymentRequest updated = paymentRequestRepository.save(paymentRequest);

        log.info("Payment request id={} DECLINED by payer {}", requestId, payer.getEmail());

        return PaymentRequestResponse.from(updated);
    }
}
