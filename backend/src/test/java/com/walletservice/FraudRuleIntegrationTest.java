package com.walletservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walletservice.dto.request.DepositRequest;
import com.walletservice.dto.request.LoginRequest;
import com.walletservice.dto.request.RegisterRequest;
import com.walletservice.dto.request.TransferRequest;
import com.walletservice.dto.response.AuthResponse;
import com.walletservice.dto.response.TransferResponse;
import com.walletservice.entity.Transfer;
import com.walletservice.entity.TransferStatus;
import com.walletservice.repository.LedgerEntryRepository;
import com.walletservice.repository.TransferRepository;
import com.walletservice.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.fraud.max-amount-per-transfer=50000",
        "app.fraud.velocity-max-transfers=3",
        "app.fraud.velocity-window-minutes=5",
        "app.rate-limit.login.capacity=1000",
        "app.rate-limit.login.refill-tokens=1000",
        "app.rate-limit.login.refill-duration-seconds=1",
        "app.rate-limit.transfer.capacity=1000",
        "app.rate-limit.transfer.refill-tokens=1000",
        "app.rate-limit.transfer.refill-duration-seconds=1"
})
@AutoConfigureMockMvc
public class FraudRuleIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    private record UserSession(String email, String token, Long walletId) {}

    private UserSession createAndFundUser(String email, Long initialDeposit) throws Exception {
        RegisterRequest registerReq = new RegisterRequest(email, "Password123!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest(email, "Password123!");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);

        MvcResult walletResult = mockMvc.perform(get("/api/v1/wallet")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();

        Long walletId = objectMapper.readTree(walletResult.getResponse().getContentAsString()).get("walletId").asLong();

        if (initialDeposit != null && initialDeposit > 0) {
            DepositRequest depositReq = new DepositRequest(initialDeposit);
            mockMvc.perform(post("/api/v1/wallet/deposit")
                            .header("Authorization", "Bearer " + auth.accessToken())
                            .header("Idempotency-Key", "dep-" + UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(depositReq)))
                .andExpect(status().isOk());
        }

        return new UserSession(email, auth.accessToken(), walletId);
    }

    @Test
    @DisplayName("Max amount boundary: exactly at limit succeeds, limit + 1 fails with 422 and is recorded as REJECTED")
    void testMaxAmountBoundary() throws Exception {
        UserSession sender = createAndFundUser("fraud-sender-" + UUID.randomUUID() + "@test.com", 200000L);
        UserSession receiver = createAndFundUser("fraud-receiver-" + UUID.randomUUID() + "@test.com", 0L);

        // 1. Boundary: exactly at max-amount-per-transfer (50,000 paise) -> SUCCESS
        TransferRequest allowedReq = new TransferRequest(receiver.email(), 50000L, "Exact max limit");
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + sender.token())
                        .header("Idempotency-Key", "idem-max-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allowedReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(50000));

        // Sender balance = 200,000 - 50,000 = 150,000
        Long senderBalanceBeforeExceeded = walletRepository.findById(sender.walletId()).orElseThrow().getBalance();
        assertThat(senderBalanceBeforeExceeded).isEqualTo(150000L);

        // 2. Boundary: exactly limit + 1 (50,001 paise) -> 422 FRAUD_LIMIT_EXCEEDED
        TransferRequest exceededReq = new TransferRequest(receiver.email(), 50001L, "Exceeds max limit by 1");
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + sender.token())
                        .header("Idempotency-Key", "idem-max-exceeded-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exceededReq)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FRAUD_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.status").value(422));

        // 3. Confirm REJECTED transfer was recorded in database
        List<Transfer> rejectedTransfers = transferRepository.findByFromWalletIdAndStatus(sender.walletId(), TransferStatus.REJECTED);
        assertThat(rejectedTransfers).isNotEmpty();
        Transfer recordedRejection = rejectedTransfers.stream()
                .filter(t -> t.getAmount().equals(50001L))
                .findFirst()
                .orElse(null);
        assertThat(recordedRejection).isNotNull();
        assertThat(recordedRejection.getStatus()).isEqualTo(TransferStatus.REJECTED);
        assertThat(recordedRejection.getReference()).isEqualTo("Exceeds max limit by 1");

        // 4. Balances and ledger unchanged
        Long senderBalanceAfter = walletRepository.findById(sender.walletId()).orElseThrow().getBalance();
        Long receiverBalanceAfter = walletRepository.findById(receiver.walletId()).orElseThrow().getBalance();
        assertThat(senderBalanceAfter).isEqualTo(150000L);
        assertThat(receiverBalanceAfter).isEqualTo(50000L);
    }

    @Test
    @DisplayName("Velocity boundary: N transfers succeed, (N+1)-th transfer fails with 422 and is recorded as REJECTED")
    void testVelocityBoundary() throws Exception {
        UserSession sender = createAndFundUser("velocity-sender-" + UUID.randomUUID() + "@test.com", 500000L);
        UserSession receiver = createAndFundUser("velocity-receiver-" + UUID.randomUUID() + "@test.com", 0L);

        // Limit configured is 3 transfers in 5 minutes
        // Execute exactly 3 transfers (boundary value) -> ALL 3 MUST SUCCEED
        for (int i = 1; i <= 3; i++) {
            TransferRequest req = new TransferRequest(receiver.email(), 1000L, "Transfer #" + i);
            mockMvc.perform(post("/api/v1/transfers")
                            .header("Authorization", "Bearer " + sender.token())
                            .header("Idempotency-Key", "idem-vel-" + i + "-" + UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        Long senderBalanceAfter3 = walletRepository.findById(sender.walletId()).orElseThrow().getBalance();
        assertThat(senderBalanceAfter3).isEqualTo(500000L - 3000L); // 497000

        // 4th transfer (boundary + 1, exceeding velocity limit of 3) -> MUST FAIL WITH 422
        TransferRequest fourthReq = new TransferRequest(receiver.email(), 1000L, "Transfer #4 (Velocity Trip)");
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + sender.token())
                        .header("Idempotency-Key", "idem-vel-4-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fourthReq)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FRAUD_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Velocity limit exceeded")));

        // Confirm 4th transfer recorded as REJECTED in database
        List<Transfer> rejectedTransfers = transferRepository.findByFromWalletIdAndStatus(sender.walletId(), TransferStatus.REJECTED);
        assertThat(rejectedTransfers).isNotEmpty();
        Transfer velocityRejection = rejectedTransfers.stream()
                .filter(t -> "Transfer #4 (Velocity Trip)".equals(t.getReference()))
                .findFirst()
                .orElse(null);
        assertThat(velocityRejection).isNotNull();
        assertThat(velocityRejection.getStatus()).isEqualTo(TransferStatus.REJECTED);

        // Balances remain untouched after rejected transfer
        Long senderBalanceFinal = walletRepository.findById(sender.walletId()).orElseThrow().getBalance();
        Long receiverBalanceFinal = walletRepository.findById(receiver.walletId()).orElseThrow().getBalance();
        assertThat(senderBalanceFinal).isEqualTo(497000L);
        assertThat(receiverBalanceFinal).isEqualTo(3000L);
    }
}
