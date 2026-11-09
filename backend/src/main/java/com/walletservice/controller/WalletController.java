package com.walletservice.controller;

import com.walletservice.dto.request.DepositRequest;
import com.walletservice.dto.response.DepositResponse;
import com.walletservice.dto.response.WalletResponse;
import com.walletservice.security.UserPrincipal;
import com.walletservice.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<WalletResponse> getCurrentUserWallet(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        WalletResponse response = walletService.getWalletByUserId(userPrincipal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit")
    public ResponseEntity<DepositResponse> deposit(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DepositRequest request) {
        DepositResponse response = walletService.deposit(userPrincipal.getId(), idempotencyKey, request);
        return ResponseEntity.ok(response);
    }
}
