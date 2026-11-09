package com.walletservice.service;

import com.walletservice.dto.request.CreateScheduledTransferRequest;
import com.walletservice.dto.request.TransferRequest;
import com.walletservice.dto.response.ScheduledTransferResponse;
import com.walletservice.entity.*;
import com.walletservice.exception.ResourceNotFoundException;
import com.walletservice.exception.SelfTransferException;
import com.walletservice.repository.ScheduledTransferRepository;
import com.walletservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ScheduledTransferService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTransferService.class);

    private final ScheduledTransferRepository scheduledTransferRepository;
    private final UserRepository userRepository;
    private final TransferOrchestrator transferOrchestrator;
    private final SseNotificationService sseNotificationService;

    public ScheduledTransferService(
            ScheduledTransferRepository scheduledTransferRepository,
            UserRepository userRepository,
            TransferOrchestrator transferOrchestrator,
            SseNotificationService sseNotificationService) {
        this.scheduledTransferRepository = scheduledTransferRepository;
        this.userRepository = userRepository;
        this.transferOrchestrator = transferOrchestrator;
        this.sseNotificationService = sseNotificationService;
    }

    @Transactional
    public ScheduledTransferResponse createSchedule(Long userId, CreateScheduledTransferRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String recipientEmail = request.recipientEmail().trim().toLowerCase();
        if (user.getEmail().equalsIgnoreCase(recipientEmail)) {
            throw new SelfTransferException();
        }

        Instant nextRunAt = request.startDate() != null
                ? request.startDate()
                : calculateInitialRunAt(request.frequency());

        ScheduledTransfer scheduledTransfer = new ScheduledTransfer(
                user, recipientEmail, request.amount(), request.frequency(), nextRunAt
        );

        ScheduledTransfer saved = scheduledTransferRepository.save(scheduledTransfer);
        log.info("Created scheduled transfer id={} for user {} -> {} (freq={}, nextRun={})",
                saved.getId(), userId, recipientEmail, request.frequency(), nextRunAt);

        return ScheduledTransferResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ScheduledTransferResponse> getUserSchedules(Long userId) {
        return scheduledTransferRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ScheduledTransferResponse::from)
                .toList();
    }

    @Transactional
    public void cancelSchedule(Long userId, Long scheduleId) {
        ScheduledTransfer schedule = scheduledTransferRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled transfer not found: " + scheduleId));

        if (!schedule.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Scheduled transfer does not belong to user");
        }

        schedule.setStatus(ScheduleStatus.CANCELLED);
        scheduledTransferRepository.save(schedule);
        log.info("Cancelled scheduled transfer id={} for user {}", scheduleId, userId);
    }

    @Scheduled(fixedRate = 60000)
    public void processDueScheduledTransfers() {
        Instant now = Instant.now();
        List<ScheduledTransfer> dueTransfers = scheduledTransferRepository.findDueTransfers(ScheduleStatus.ACTIVE, now);

        if (dueTransfers.isEmpty()) {
            return;
        }

        log.info("Processing {} due scheduled transfers at {}", dueTransfers.size(), now);

        for (ScheduledTransfer st : dueTransfers) {
            String idemKey = "sched-" + st.getId() + "-" + st.getNextRunAt().getEpochSecond();
            try {
                transferOrchestrator.transfer(
                        st.getUser().getId(),
                        idemKey,
                        new TransferRequest(st.getRecipientEmail(), st.getAmount(), "Recurring " + st.getFrequency() + " Transfer")
                );

                st.setLastRunAt(now);
                st.setNextRunAt(calculateNextRunAt(st.getNextRunAt(), st.getFrequency()));
                scheduledTransferRepository.save(st);

                log.info("Successfully executed scheduled transfer id={}, next run at {}", st.getId(), st.getNextRunAt());
            } catch (Exception e) {
                log.error("Failed to execute scheduled transfer id={}: {}", st.getId(), e.getMessage());
                // If it failed, advance nextRun to avoid infinite tight looping
                st.setLastRunAt(now);
                st.setNextRunAt(calculateNextRunAt(st.getNextRunAt(), st.getFrequency()));
                scheduledTransferRepository.save(st);
            }
        }
    }

    private Instant calculateInitialRunAt(ScheduleFrequency frequency) {
        Instant now = Instant.now();
        return switch (frequency) {
            case DAILY -> now.plus(1, ChronoUnit.DAYS);
            case WEEKLY -> now.plus(7, ChronoUnit.DAYS);
            case MONTHLY -> now.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
        };
    }

    private Instant calculateNextRunAt(Instant currentRunAt, ScheduleFrequency frequency) {
        return switch (frequency) {
            case DAILY -> currentRunAt.plus(1, ChronoUnit.DAYS);
            case WEEKLY -> currentRunAt.plus(7, ChronoUnit.DAYS);
            case MONTHLY -> currentRunAt.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
        };
    }
}
