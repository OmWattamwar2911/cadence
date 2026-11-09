package com.walletservice.controller;

import com.walletservice.dto.request.CreateScheduledTransferRequest;
import com.walletservice.dto.response.ScheduledTransferResponse;
import com.walletservice.security.UserPrincipal;
import com.walletservice.service.ScheduledTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/scheduled-transfers")
@Tag(name = "Scheduled Transfers", description = "Standing orders and recurring automated transfers")
@SecurityRequirement(name = "bearerAuth")
public class ScheduledTransferController {

    private final ScheduledTransferService scheduledTransferService;

    public ScheduledTransferController(ScheduledTransferService scheduledTransferService) {
        this.scheduledTransferService = scheduledTransferService;
    }

    @PostMapping
    @Operation(summary = "Set up a new recurring automated transfer")
    public ResponseEntity<ScheduledTransferResponse> createSchedule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreateScheduledTransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduledTransferService.createSchedule(userPrincipal.getId(), request));
    }

    @GetMapping
    @Operation(summary = "List all active and past recurring transfer schedules for the current user")
    public ResponseEntity<List<ScheduledTransferResponse>> getSchedules(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(scheduledTransferService.getUserSchedules(userPrincipal.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel an active scheduled transfer")
    public ResponseEntity<Void> cancelSchedule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        scheduledTransferService.cancelSchedule(userPrincipal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
