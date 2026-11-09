package com.walletservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walletservice.dto.request.DepositRequest;
import com.walletservice.dto.request.LoginRequest;
import com.walletservice.dto.request.RegisterRequest;
import com.walletservice.entity.EntryType;
import com.walletservice.entity.LedgerEntry;
import com.walletservice.entity.Wallet;
import com.walletservice.repository.LedgerEntryRepository;
import com.walletservice.repository.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class WalletIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    private String registerAndLogin(String email, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(email, password);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    @DisplayName("Auto-create wallet on registration in same transaction with balance 0")
    void register_autoCreatesWallet_withZeroBalance() throws Exception {
        String email = "wallet_user_" + UUID.randomUUID() + "@example.com";
        String token = registerAndLogin(email, "Password123!");

        // 1. Verify via API
        mockMvc.perform(get("/api/v1/wallet")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId", notNullValue()))
                .andExpect(jsonPath("$.balance", is(0)))
                .andExpect(jsonPath("$.formattedBalance", is("₹0.00")))
                .andExpect(jsonPath("$.currency", is("INR")));

        // 2. Verify directly in database
        Wallet wallet = walletRepository.findByUserEmail(email).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(0L);
        assertThat(wallet.getCurrency()).isEqualTo("INR");
    }

    @Test
    @DisplayName("Deposit increases balance and writes a ledger entry with correct balance_after")
    void deposit_increasesBalance_andWritesLedgerEntry() throws Exception {
        String email = "deposit_user_" + UUID.randomUUID() + "@example.com";
        String token = registerAndLogin(email, "Password123!");

        String idemKey = "idem-" + UUID.randomUUID();
        DepositRequest depositRequest = new DepositRequest(50000L); // ₹500.00

        mockMvc.perform(post("/api/v1/wallet/deposit")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depositAmount", is(50000)))
                .andExpect(jsonPath("$.newBalance", is(50000)))
                .andExpect(jsonPath("$.currency", is("INR")))
                .andExpect(jsonPath("$.reference", notNullValue()));

        // Verify wallet updated
        Wallet wallet = walletRepository.findByUserEmail(email).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(50000L);

        // Verify ledger entry
        List<LedgerEntry> entries = ledgerEntryRepository.findAll().stream()
                .filter(le -> le.getWallet().getId().equals(wallet.getId()))
                .toList();

        assertThat(entries).hasSize(1);
        LedgerEntry entry = entries.get(0);
        assertThat(entry.getEntryType()).isEqualTo(EntryType.DEPOSIT);
        assertThat(entry.getAmount()).isEqualTo(50000L);
        assertThat(entry.getBalanceAfter()).isEqualTo(50000L);
        assertThat(entry.getTransfer()).isNull();
    }

    @Test
    @DisplayName("Idempotent deposit: repeating with same key returns stored response without moving money again")
    void deposit_idempotentReplay_doesNotMoveMoneyAgain() throws Exception {
        String email = "replay_user_" + UUID.randomUUID() + "@example.com";
        String token = registerAndLogin(email, "Password123!");

        String idemKey = "idem-replay-" + UUID.randomUUID();
        DepositRequest depositRequest = new DepositRequest(30000L); // ₹300.00

        // 1. First execution
        MvcResult firstResult = mockMvc.perform(post("/api/v1/wallet/deposit")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newBalance", is(30000)))
                .andReturn();

        String firstReference = objectMapper.readTree(firstResult.getResponse().getContentAsString())
                .get("reference").asText();

        // 2. Replay with exact same key and payload
        MvcResult replayResult = mockMvc.perform(post("/api/v1/wallet/deposit")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newBalance", is(30000)))
                .andReturn();

        String replayReference = objectMapper.readTree(replayResult.getResponse().getContentAsString())
                .get("reference").asText();

        // References must match (cached response replayed)
        assertThat(replayReference).isEqualTo(firstReference);

        // Balance must remain 30000 (NOT 60000)
        Wallet wallet = walletRepository.findByUserEmail(email).orElseThrow();
        assertThat(wallet.getBalance()).isEqualTo(30000L);

        // Ledger must contain exactly 1 entry
        List<LedgerEntry> entries = ledgerEntryRepository.findAll().stream()
                .filter(le -> le.getWallet().getId().equals(wallet.getId()))
                .toList();
        assertThat(entries).hasSize(1);
    }

    @Test
    @DisplayName("Idempotency violation: same key with different payload returns 422 Unprocessable Entity")
    void deposit_idempotencyConflict_differentPayload_returns422() throws Exception {
        String email = "conflict_user_" + UUID.randomUUID() + "@example.com";
        String token = registerAndLogin(email, "Password123!");

        String idemKey = "idem-conflict-" + UUID.randomUUID();

        // 1. Initial request with 20000
        mockMvc.perform(post("/api/v1/wallet/deposit")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepositRequest(20000L))))
                .andExpect(status().isOk());

        // 2. Reuse key with different amount (40000)
        mockMvc.perform(post("/api/v1/wallet/deposit")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepositRequest(40000L))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.code", is("IDEMPOTENCY_KEY_REUSED")));
    }

    @Test
    @DisplayName("Invalid deposit amounts: 0 rejected with 400 Bad Request")
    void deposit_invalidAmount_zero_returns400() throws Exception {
        String email = "val_zero_" + UUID.randomUUID() + "@example.com";
        String token = registerAndLogin(email, "Password123!");

        mockMvc.perform(post("/api/v1/wallet/deposit")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "idem-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("Invalid deposit amounts: negative rejected with 400 Bad Request")
    void deposit_invalidAmount_negative_returns400() throws Exception {
        String email = "val_neg_" + UUID.randomUUID() + "@example.com";
        String token = registerAndLogin(email, "Password123!");

        mockMvc.perform(post("/api/v1/wallet/deposit")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "idem-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": -500}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("Invalid deposit amounts: huge number exceeding max limit rejected with 400 Bad Request")
    void deposit_invalidAmount_huge_returns400() throws Exception {
        String email = "val_huge_" + UUID.randomUUID() + "@example.com";
        String token = registerAndLogin(email, "Password123!");

        mockMvc.perform(post("/api/v1/wallet/deposit")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "idem-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 999999999999999999}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("Invalid deposit amounts: non-numeric string rejected with 400 Bad Request")
    void deposit_invalidAmount_nonNumeric_returns400() throws Exception {
        String email = "val_str_" + UUID.randomUUID() + "@example.com";
        String token = registerAndLogin(email, "Password123!");

        mockMvc.perform(post("/api/v1/wallet/deposit")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "idem-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": \"not-a-number\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("MALFORMED_REQUEST")));
    }

    @Test
    @DisplayName("Missing Idempotency-Key header rejected with 400 Bad Request")
    void deposit_missingIdempotencyKey_returns400() throws Exception {
        String email = "val_header_" + UUID.randomUUID() + "@example.com";
        String token = registerAndLogin(email, "Password123!");

        mockMvc.perform(post("/api/v1/wallet/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 10000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("MISSING_HEADER")));
    }

    @Test
    @DisplayName("User A cannot see User B's wallet (strict user data isolation)")
    void userA_cannotSee_userBsWallet() throws Exception {
        // User A registers and deposits ₹1,000.00 (100000 paise)
        String userAEmail = "usera_" + UUID.randomUUID() + "@example.com";
        String tokenA = registerAndLogin(userAEmail, "Password123!");

        mockMvc.perform(post("/api/v1/wallet/deposit")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("Idempotency-Key", "idem-usera-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 100000}"))
                .andExpect(status().isOk());

        // User B registers
        String userBEmail = "userb_" + UUID.randomUUID() + "@example.com";
        String tokenB = registerAndLogin(userBEmail, "Password123!");

        // User B requests their wallet
        MvcResult userBWalletResult = mockMvc.perform(get("/api/v1/wallet")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(0)))
                .andExpect(jsonPath("$.formattedBalance", is("₹0.00")))
                .andReturn();

        // User A requests their wallet
        MvcResult userAWalletResult = mockMvc.perform(get("/api/v1/wallet")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(100000)))
                .andExpect(jsonPath("$.formattedBalance", is("₹1000.00")))
                .andReturn();

        Long walletIdA = objectMapper.readTree(userAWalletResult.getResponse().getContentAsString())
                .get("walletId").asLong();
        Long walletIdB = objectMapper.readTree(userBWalletResult.getResponse().getContentAsString())
                .get("walletId").asLong();

        // Verify distinct wallet IDs and isolation
        assertThat(walletIdA).isNotEqualTo(walletIdB);
    }
}
