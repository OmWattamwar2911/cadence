package com.walletservice.controller;

import com.walletservice.dto.response.LedgerAuditResponse;
import com.walletservice.dto.response.TransferReceiptResponse;
import com.walletservice.security.UserPrincipal;
import com.walletservice.service.LedgerAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ledger")
@Tag(name = "Ledger Audit", description = "Cryptographic ledger verification and receipts")
@SecurityRequirement(name = "bearerAuth")
public class LedgerAuditController {

    private final LedgerAuditService ledgerAuditService;

    public LedgerAuditController(LedgerAuditService ledgerAuditService) {
        this.ledgerAuditService = ledgerAuditService;
    }

    @GetMapping("/audit")
    @Operation(summary = "Cryptographically audit the user's ledger hash chain")
    public ResponseEntity<LedgerAuditResponse> auditWallet(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ledgerAuditService.auditUserWallet(userPrincipal.getId()));
    }

    @GetMapping("/receipts/{reference}")
    @Operation(summary = "Get a certified cryptographic transfer receipt")
    public ResponseEntity<TransferReceiptResponse> getReceipt(
            @PathVariable String reference,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(ledgerAuditService.getTransferReceipt(reference, userPrincipal.getId()));
    }
}
