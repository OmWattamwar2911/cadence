package com.walletservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walletservice.dto.request.DepositRequest;
import com.walletservice.dto.request.LoginRequest;
import com.walletservice.dto.request.RegisterRequest;
import com.walletservice.dto.request.TransferRequest;
import com.walletservice.entity.*;
import com.walletservice.repository.*;
import com.walletservice.service.TransferExecutionHook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TransferIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private TransferExecutionHook transferExecutionHook;

    @AfterEach
    void tearDown() {
        transferExecutionHook.clearHook();
    }

    private record UserSession(Long userId, String email, String token, Long walletId) {}

    private UserSession createAndFundUser(String prefix, long initialDeposit) throws Exception {
        String email = prefix + "_" + UUID.randomUUID() + "@example.com";
        String password = "Password123!";

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, password))))
                .andExpect(status().isCreated())
                .andReturn();

        Long userId = objectMapper.readTree(regResult.getResponse().getContentAsString()).get("userId").asLong();
        Long walletId = objectMapper.readTree(regResult.getResponse().getContentAsString()).get("walletId").asLong();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();

        if (initialDeposit > 0) {
            mockMvc.perform(post("/api/v1/wallet/deposit")
                            .header("Authorization", "Bearer " + token)
                            .header("Idempotency-Key", "fund-" + UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new DepositRequest(initialDeposit))))
                    .andExpect(status().isOk());
        }

        return new UserSession(userId, email, token, walletId);
    }

    @Test
    @DisplayName("a) Successful transfer updates both balances and writes 2 ledger rows")
    void transfer_successful_updatesBothBalancesAndWritesLedgerRows() throws Exception {
        UserSession alice = createAndFundUser("alice_a", 100000L); // ₹1,000.00
        UserSession bob = createAndFundUser("bob_a", 20000L);     // ₹200.00

        String idemKey = "idem-" + UUID.randomUUID();
        TransferRequest request = new TransferRequest(bob.email(), 30000L, "Dinner split"); // ₹300.00

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + alice.token())
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferId", notNullValue()))
                .andExpect(jsonPath("$.fromWalletId", is(alice.walletId().intValue())))
                .andExpect(jsonPath("$.toWalletId", is(bob.walletId().intValue())))
                .andExpect(jsonPath("$.amount", is(30000)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.senderBalanceAfter", is(70000)));

        // 1. Verify balances in DB
        Wallet aliceWallet = walletRepository.findById(alice.walletId()).orElseThrow();
        Wallet bobWallet = walletRepository.findById(bob.walletId()).orElseThrow();

        assertThat(aliceWallet.getBalance()).isEqualTo(70000L);
        assertThat(bobWallet.getBalance()).isEqualTo(50000L);

        // 2. Verify ledger entries
        List<LedgerEntry> aliceLedger = ledgerEntryRepository.findAll().stream()
                .filter(le -> le.getWallet().getId().equals(alice.walletId()) && le.getEntryType() == EntryType.DEBIT)
                .toList();
        List<LedgerEntry> bobLedger = ledgerEntryRepository.findAll().stream()
                .filter(le -> le.getWallet().getId().equals(bob.walletId()) && le.getEntryType() == EntryType.CREDIT)
                .toList();

        assertThat(aliceLedger).hasSize(1);
        assertThat(aliceLedger.get(0).getAmount()).isEqualTo(30000L);
        assertThat(aliceLedger.get(0).getBalanceAfter()).isEqualTo(70000L);

        assertThat(bobLedger).hasSize(1);
        assertThat(bobLedger.get(0).getAmount()).isEqualTo(30000L);
        assertThat(bobLedger.get(0).getBalanceAfter()).isEqualTo(50000L);

        // Both point to the exact same transfer record
        assertThat(aliceLedger.get(0).getTransfer().getId())
                .isEqualTo(bobLedger.get(0).getTransfer().getId());
    }

    @Test
    @DisplayName("b) Insufficient funds -> 422, balances unchanged")
    void transfer_insufficientFunds_returns422_balancesUnchanged() throws Exception {
        UserSession alice = createAndFundUser("alice_b", 10000L); // ₹100.00
        UserSession bob = createAndFundUser("bob_b", 0L);

        TransferRequest request = new TransferRequest(bob.email(), 50000L, "Rent"); // Requires ₹500.00

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + alice.token())
                        .header("Idempotency-Key", "idem-b-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_FUNDS")));

        // Balances remain untouched
        Wallet aliceWallet = walletRepository.findById(alice.walletId()).orElseThrow();
        Wallet bobWallet = walletRepository.findById(bob.walletId()).orElseThrow();
        assertThat(aliceWallet.getBalance()).isEqualTo(10000L);
        assertThat(bobWallet.getBalance()).isEqualTo(0L);
    }

    @Test
    @DisplayName("c) Sender == receiver -> 400; unknown receiver -> 404")
    void transfer_invalidRecipients_returnsExpectedCodes() throws Exception {
        UserSession alice = createAndFundUser("alice_c", 50000L);

        // 1. Sender == Receiver -> 400
        TransferRequest selfRequest = new TransferRequest(alice.email(), 1000L, "Self transfer");
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + alice.token())
                        .header("Idempotency-Key", "idem-c1-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("SELF_TRANSFER_NOT_ALLOWED")));

        // 2. Unknown receiver -> 404
        TransferRequest unknownRequest = new TransferRequest("nobody_exists_12345@example.com", 1000L, "Ghost");
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + alice.token())
                        .header("Idempotency-Key", "idem-c2-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("d) Same Idempotency-Key sent twice sequentially -> identical response, money moved once")
    void transfer_idempotency_sequentialDuplicate_returnsCachedResponse() throws Exception {
        UserSession alice = createAndFundUser("alice_d", 100000L);
        UserSession bob = createAndFundUser("bob_d", 0L);

        String idemKey = "idem-d-" + UUID.randomUUID();
        TransferRequest request = new TransferRequest(bob.email(), 40000L, "Coffee");

        // First execution
        MvcResult firstResult = mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + alice.token())
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderBalanceAfter", is(60000)))
                .andReturn();

        Long firstTransferId = objectMapper.readTree(firstResult.getResponse().getContentAsString()).get("transferId").asLong();

        // Second execution (replay)
        MvcResult secondResult = mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + alice.token())
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderBalanceAfter", is(60000)))
                .andReturn();

        Long secondTransferId = objectMapper.readTree(secondResult.getResponse().getContentAsString()).get("transferId").asLong();

        assertThat(secondTransferId).isEqualTo(firstTransferId);

        // Balances verified: exactly 40000 moved
        assertThat(walletRepository.findById(alice.walletId()).orElseThrow().getBalance()).isEqualTo(60000L);
        assertThat(walletRepository.findById(bob.walletId()).orElseThrow().getBalance()).isEqualTo(40000L);

        // Exactly 1 transfer row in database
        long transferCount = transferRepository.findAll().stream()
                .filter(t -> t.getFromWallet().getId().equals(alice.walletId()))
                .count();
        assertThat(transferCount).isEqualTo(1);
    }

    @Test
    @DisplayName("e) Same key sent 10 times CONCURRENTLY -> exactly one transfer row, money moved once")
    void transfer_idempotency_concurrentDuplicate_movesMoneyOnce() throws Exception {
        UserSession alice = createAndFundUser("alice_e", 100000L);
        UserSession bob = createAndFundUser("bob_e", 0L);

        String sharedIdemKey = "concurrent-idem-" + UUID.randomUUID();
        TransferRequest request = new TransferRequest(bob.email(), 25000L, "Shared split");

        int concurrency = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrency);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger inProgressCount = new AtomicInteger(0);

        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult res = mockMvc.perform(post("/api/v1/transfers")
                                    .header("Authorization", "Bearer " + alice.token())
                                    .header("Idempotency-Key", sharedIdemKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn();

                    int status = res.getResponse().getStatus();
                    if (status == 200) {
                        successCount.incrementAndGet();
                    } else if (status == 409) {
                        inProgressCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // unexpected error
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completedInTime = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completedInTime).isTrue();
        // At least one thread succeeded; any conflicting thread received either 200 (if winner committed first) or 409
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
        assertThat(successCount.get() + inProgressCount.get()).isEqualTo(concurrency);

        // Money moved EXACTLY once: 100000 - 25000 = 75000
        Wallet aliceWallet = walletRepository.findById(alice.walletId()).orElseThrow();
        Wallet bobWallet = walletRepository.findById(bob.walletId()).orElseThrow();

        assertThat(aliceWallet.getBalance()).isEqualTo(75000L);
        assertThat(bobWallet.getBalance()).isEqualTo(25000L);

        // Exactly one transfer row exists
        long transferRows = transferRepository.findAll().stream()
                .filter(t -> t.getFromWallet().getId().equals(alice.walletId()))
                .count();
        assertThat(transferRows).isEqualTo(1);
    }

    @Test
    @DisplayName("f) Same key with different body -> 422 Unprocessable Entity")
    void transfer_idempotency_differentBody_returns422() throws Exception {
        UserSession alice = createAndFundUser("alice_f", 100000L);
        UserSession bob = createAndFundUser("bob_f", 0L);

        String idemKey = "idem-f-" + UUID.randomUUID();

        // 1. Initial transfer with 20000
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + alice.token())
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferRequest(bob.email(), 20000L, "First"))))
                .andExpect(status().isOk());

        // 2. Reuse key with different amount (30000)
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + alice.token())
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferRequest(bob.email(), 30000L, "Second"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.code", is("IDEMPOTENCY_KEY_REUSED")));
    }

    @Test
    @DisplayName("g) & i) 50 concurrent transfers in BOTH directions -> no deadlocks, no negative balance, money conserved, ledger reconciliation")
    void transfer_concurrentBidirectional_noDeadlocks_and_ledgerReconciliation() throws Exception {
        UserSession userA = createAndFundUser("bidir_a", 100000L); // ₹1,000.00
        UserSession userB = createAndFundUser("bidir_b", 100000L); // ₹1,000.00

        long totalInitialMoney = 200000L;

        int totalTransfers = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalTransfers);

        AtomicInteger successCounter = new AtomicInteger(0);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < totalTransfers; i++) {
            final boolean aToB = (i % 2 == 0);
            final long amount = 1000L; // ₹10.00 per transfer
            final String idemKey = "bidir-idem-" + i + "-" + UUID.randomUUID();

            executor.submit(() -> {
                try {
                    startLatch.await();
                    UserSession sender = aToB ? userA : userB;
                    String recipientEmail = aToB ? userB.email() : userA.email();

                    TransferRequest req = new TransferRequest(recipientEmail, amount, "Bidir transfer");

                    MvcResult res = mockMvc.perform(post("/api/v1/transfers")
                                    .header("Authorization", "Bearer " + sender.token())
                                    .header("Idempotency-Key", idemKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();

                    if (res.getResponse().getStatus() == 200) {
                        successCounter.incrementAndGet();
                    } else {
                        throw new RuntimeException("HTTP " + res.getResponse().getStatus() + ": " + res.getResponse().getContentAsString());
                    }
                } catch (Throwable t) {
                    exceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = doneLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).as("All concurrent transfers must complete within timeout").isTrue();
        assertThat(exceptions).as("Zero deadlock or server exceptions").isEmpty();
        assertThat(successCounter.get()).isEqualTo(50);

        // Verify balances & conservation of money
        Wallet walletA = walletRepository.findById(userA.walletId()).orElseThrow();
        Wallet walletB = walletRepository.findById(userB.walletId()).orElseThrow();

        assertThat(walletA.getBalance()).isGreaterThanOrEqualTo(0L);
        assertThat(walletB.getBalance()).isGreaterThanOrEqualTo(0L);
        assertThat(walletA.getBalance() + walletB.getBalance()).isEqualTo(totalInitialMoney);

        // i) Ledger Reconciliation Test:
        // Invariant: for every wallet, balance == SUM(credits and deposits) - SUM(debits)
        Long reconciledA = ledgerEntryRepository.calculateReconciledBalance(walletA.getId());
        Long reconciledB = ledgerEntryRepository.calculateReconciledBalance(walletB.getId());

        assertThat(walletA.getBalance()).isEqualTo(reconciledA);
        assertThat(walletB.getBalance()).isEqualTo(reconciledB);
    }

    @Test
    @DisplayName("h) Forced exception mid-transfer -> full rollback, balances and ledger unchanged")
    void transfer_forcedMidTransferException_rollsBackCompletely() throws Exception {
        UserSession alice = createAndFundUser("alice_h", 100000L);
        UserSession bob = createAndFundUser("bob_h", 50000L);

        // Set hook to throw right after balances are updated but before commit
        transferExecutionHook.setOnAfterDebitAction((sender, receiver) -> {
            throw new RuntimeException("Simulated catastrophic crash mid-transfer");
        });

        String idemKey = "idem-crash-" + UUID.randomUUID();
        TransferRequest request = new TransferRequest(bob.email(), 30000L, "Should fail and rollback");

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + alice.token())
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.code", is("INTERNAL_SERVER_ERROR")));

        // 1. Verify balances are COMPLETELY unchanged
        Wallet aliceWallet = walletRepository.findById(alice.walletId()).orElseThrow();
        Wallet bobWallet = walletRepository.findById(bob.walletId()).orElseThrow();
        assertThat(aliceWallet.getBalance()).isEqualTo(100000L);
        assertThat(bobWallet.getBalance()).isEqualTo(50000L);

        // 2. Verify no transfer row was committed
        long transferCount = transferRepository.findAll().stream()
                .filter(t -> t.getFromWallet().getId().equals(alice.walletId()))
                .count();
        assertThat(transferCount).isEqualTo(0);

        // 3. Verify no debit ledger entry exists for alice
        long debitEntries = ledgerEntryRepository.findAll().stream()
                .filter(le -> le.getWallet().getId().equals(alice.walletId()) && le.getEntryType() == EntryType.DEBIT)
                .count();
        assertThat(debitEntries).isEqualTo(0);
    }
}
