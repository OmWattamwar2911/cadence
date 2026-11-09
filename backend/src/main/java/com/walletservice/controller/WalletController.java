package com.walletservice.controller;

import com.walletservice.dto.response.WalletResponse;
import com.walletservice.security.UserPrincipal;
import com.walletservice.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
