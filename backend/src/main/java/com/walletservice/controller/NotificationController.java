package com.walletservice.controller;

import com.walletservice.security.UserPrincipal;
import com.walletservice.service.SseNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Real-time Server-Sent Events stream")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final SseNotificationService sseNotificationService;

    public NotificationController(SseNotificationService sseNotificationService) {
        this.sseNotificationService = sseNotificationService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to live user notifications via Server-Sent Events")
    public SseEmitter stream(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return sseNotificationService.subscribe(userPrincipal.getId());
    }
}
