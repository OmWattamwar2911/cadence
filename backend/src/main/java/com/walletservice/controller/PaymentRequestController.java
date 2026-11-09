package com.walletservice.controller;

import com.walletservice.dto.request.CreatePaymentRequest;
import com.walletservice.dto.response.PaymentRequestResponse;
import com.walletservice.security.UserPrincipal;
import com.walletservice.service.PaymentRequestService;
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
@RequestMapping("/api/v1/payment-requests")
@Tag(name = "Payment Requests", description = "Split bill and money request flows")
@SecurityRequirement(name = "bearerAuth")
public class PaymentRequestController {

    private final PaymentRequestService paymentRequestService;

    public PaymentRequestController(PaymentRequestService paymentRequestService) {
        this.paymentRequestService = paymentRequestService;
    }

    @PostMapping
    @Operation(summary = "Create a new money request or split bill request")
    public ResponseEntity<PaymentRequestResponse> createRequest(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentRequestService.createRequest(userPrincipal.getId(), request));
    }

    @GetMapping("/incoming")
    @Operation(summary = "Get pending incoming payment requests addressed to the authenticated user")
    public ResponseEntity<List<PaymentRequestResponse>> getIncoming(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(paymentRequestService.getIncomingPendingRequests(userPrincipal.getUsername()));
    }

    @GetMapping("/outgoing")
    @Operation(summary = "Get all payment requests initiated by the authenticated user")
    public ResponseEntity<List<PaymentRequestResponse>> getOutgoing(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(paymentRequestService.getOutgoingRequests(userPrincipal.getId()));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Approve and pay an incoming payment request")
    public ResponseEntity<PaymentRequestResponse> payRequest(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ResponseEntity.ok(paymentRequestService.payRequest(userPrincipal.getId(), id, idempotencyKey));
    }

    @PostMapping("/{id}/decline")
    @Operation(summary = "Decline an incoming payment request")
    public ResponseEntity<PaymentRequestResponse> declineRequest(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        return ResponseEntity.ok(paymentRequestService.declineRequest(userPrincipal.getId(), id));
    }
}
