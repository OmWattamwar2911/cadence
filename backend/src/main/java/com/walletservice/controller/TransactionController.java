package com.walletservice.controller;

import com.walletservice.dto.response.PageResponse;
import com.walletservice.dto.response.TransactionResponse;
import com.walletservice.entity.EntryType;
import com.walletservice.security.UserPrincipal;
import com.walletservice.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/transactions")
@Validated
@Tag(name = "Transactions", description = "Transaction history and ledger entry queries")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    @Operation(summary = "Get paginated transaction history with optional type and date filters")
    public ResponseEntity<PageResponse<TransactionResponse>> getTransactions(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index cannot be negative") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Page size must be at least 1") @Max(value = 100, message = "Page size cannot exceed 100") int size,
            @RequestParam(required = false) EntryType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        PageResponse<TransactionResponse> response = transactionService.getTransactions(
                userPrincipal.getId(), type, from, to, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export official financial statement in CSV format")
    public ResponseEntity<byte[]> exportStatement(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        byte[] csv = transactionService.exportStatementCsv(userPrincipal.getId());
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cadence-statement.csv\"")
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv")
                .body(csv);
    }
}
