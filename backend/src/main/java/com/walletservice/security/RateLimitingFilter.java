package com.walletservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walletservice.dto.response.ErrorResponse;
import com.walletservice.filter.RequestIdFilter;
import com.walletservice.service.RateLimitingService;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RateLimitingService rateLimitingService, ObjectMapper objectMapper) {
        this.rateLimitingService = rateLimitingService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. Rate limiting on auth endpoints (POST /api/v1/auth/login, POST /api/v1/auth/register)
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/v1/auth/")) {
            String clientIp = getClientIp(request);
            ConsumptionProbe probe = rateLimitingService.tryConsumeAuth(clientIp);
            if (!probe.isConsumed()) {
                sendRateLimitError(request, response, probe, clientIp);
                return;
            }
        }

        // 2. Rate limiting on transfer endpoints (POST /api/v1/transfers)
        if ("POST".equalsIgnoreCase(method) && path.equals("/api/v1/transfers")) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String clientKey;
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                clientKey = "user:" + principal.getId();
            } else {
                clientKey = "ip:" + getClientIp(request);
            }

            ConsumptionProbe probe = rateLimitingService.tryConsumeTransfer(clientKey);
            if (!probe.isConsumed()) {
                sendRateLimitError(request, response, probe, clientKey);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendRateLimitError(HttpServletRequest request,
                                    HttpServletResponse response,
                                    ConsumptionProbe probe,
                                    String clientKey) throws IOException {
        long nanos = probe.getNanosToWaitForRefill();
        long retryAfter = Math.max(1, (long) Math.ceil(nanos / 1_000_000_000.0));

        log.warn("Rate limit exceeded for key '{}' on path '{}'. Retry-After: {} seconds",
                clientKey, request.getRequestURI(), retryAfter);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId == null) {
            requestId = (String) request.getAttribute(RequestIdFilter.MDC_KEY);
        }

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "RATE_LIMIT_EXCEEDED",
                "Rate limit exceeded. Please retry after " + retryAfter + " seconds.",
                request.getRequestURI(),
                requestId
        );

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
