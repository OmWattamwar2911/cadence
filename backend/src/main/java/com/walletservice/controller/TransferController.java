package com.walletservice.controller;

import com.walletservice.dto.request.TransferRequest;
import com.walletservice.dto.response.TransferResponse;
import com.walletservice.security.UserPrincipal;
import com.walletservice.service.TransferOrchestrator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferOrchestrator transferOrchestrator;

    public TransferController(TransferOrchestrator transferOrchestrator) {
        this.transferOrchestrator = transferOrchestrator;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        TransferResponse response = transferOrchestrator.transfer(userPrincipal.getId(), idempotencyKey, request);
        return ResponseEntity.ok(response);
    }
}
