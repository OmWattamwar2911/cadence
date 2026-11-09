package com.walletservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walletservice.dto.request.DepositRequest;
import com.walletservice.dto.request.LoginRequest;
import com.walletservice.dto.request.RegisterRequest;
import com.walletservice.dto.request.TransferRequest;
import com.walletservice.dto.response.AuthResponse;
import com.walletservice.service.RateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.rate-limit.auth.capacity=3",
        "app.rate-limit.auth.refill-tokens=3",
        "app.rate-limit.auth.refill-duration-seconds=60",
        "app.rate-limit.transfer.capacity=2",
        "app.rate-limit.transfer.refill-tokens=2",
        "app.rate-limit.transfer.refill-duration-seconds=60",
        "app.fraud.max-amount-per-transfer=10000000",
        "app.fraud.velocity-max-transfers=1000",
        "app.fraud.velocity-window-minutes=5"
})
@AutoConfigureMockMvc
public class RateLimitingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService.clearBuckets();
    }

    @Test
    @DisplayName("Auth endpoint rate limiting: 3 requests allowed, 4th returns 429 with Retry-After header")
    void testAuthRateLimiting() throws Exception {
        String testIp = "192.168.1.100";
        LoginRequest badLogin = new LoginRequest("unknown@test.com", "WrongPassword");

        // 1st request -> 401 Unauthorized (consumed token 1)
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> { request.setRemoteAddr(testIp); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());

        // 2nd request -> 401 Unauthorized (consumed token 2)
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> { request.setRemoteAddr(testIp); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());

        // 3rd request -> 401 Unauthorized (consumed token 3 - exact capacity boundary)
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> { request.setRemoteAddr(testIp); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());

        // 4th request -> 429 Too Many Requests (capacity exceeded)
        MvcResult rateLimitedResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> { request.setRemoteAddr(testIp); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
                .andReturn();

        String retryAfterHeader = rateLimitedResult.getResponse().getHeader("Retry-After");
        assertThat(retryAfterHeader).isNotNull();
        long retryAfterSeconds = Long.parseLong(retryAfterHeader);
        assertThat(retryAfterSeconds).isGreaterThan(0L);

        // A different IP is NOT blocked
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> { request.setRemoteAddr("192.168.1.200"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized()); // Not 429!
    }

    @Test
    @DisplayName("Transfer endpoint rate limiting: 2 transfers allowed per user, 3rd returns 429 with Retry-After header")
    void testTransferRateLimiting() throws Exception {
        // Register sender and receiver with unique IPs so registration doesn't exhaust auth limit
        String senderEmail = "ratelimit-sender-" + UUID.randomUUID() + "@test.com";
        String receiverEmail = "ratelimit-receiver-" + UUID.randomUUID() + "@test.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(request -> { request.setRemoteAddr("10.0.0.1"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(senderEmail, "Password123!"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(request -> { request.setRemoteAddr("10.0.0.2"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(receiverEmail, "Password123!"))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(request -> { request.setRemoteAddr("10.0.0.1"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(senderEmail, "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class).accessToken();

        // Deposit funds
        mockMvc.perform(post("/api/v1/wallet/deposit")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "dep-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepositRequest(100000L))))
                .andExpect(status().isOk());

        // Transfer capacity is 2 per user
        // 1st transfer -> 200 OK
        TransferRequest req1 = new TransferRequest(receiverEmail, 1000L, "Ref 1");
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "trf-rl-1-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        // 2nd transfer -> 200 OK (boundary at capacity)
        TransferRequest req2 = new TransferRequest(receiverEmail, 1000L, "Ref 2");
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "trf-rl-2-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk());

        // 3rd transfer -> 429 Too Many Requests
        TransferRequest req3 = new TransferRequest(receiverEmail, 1000L, "Ref 3");
        MvcResult transferRlResult = mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "trf-rl-3-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
                .andReturn();

        String retryAfterHeader = transferRlResult.getResponse().getHeader("Retry-After");
        assertThat(retryAfterHeader).isNotNull();
        assertThat(Long.parseLong(retryAfterHeader)).isGreaterThan(0L);
    }
}
