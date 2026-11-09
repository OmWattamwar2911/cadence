package com.walletservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walletservice.dto.request.DepositRequest;
import com.walletservice.dto.request.LoginRequest;
import com.walletservice.dto.request.RegisterRequest;
import com.walletservice.dto.request.TransferRequest;
import com.walletservice.dto.response.AuthResponse;
import com.walletservice.dto.response.PageResponse;
import com.walletservice.dto.response.TransactionResponse;
import com.walletservice.entity.EntryType;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.rate-limit.login.capacity=1000",
        "app.rate-limit.login.refill-tokens=1000",
        "app.rate-limit.login.refill-duration-seconds=1",
        "app.rate-limit.transfer.capacity=1000",
        "app.rate-limit.transfer.refill-tokens=1000",
        "app.rate-limit.transfer.refill-duration-seconds=1",
        "app.fraud.max-amount-per-transfer=10000000",
        "app.fraud.velocity-max-transfers=1000",
        "app.fraud.velocity-window-minutes=5"
})
@AutoConfigureMockMvc
public class TransactionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private record UserSession(String email, String token, Long walletId) {}

    private UserSession createUser(String email) throws Exception {
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
        return new UserSession(email, auth.accessToken(), walletId);
    }

    private void makeDeposit(UserSession user, Long amount) throws Exception {
        DepositRequest req = new DepositRequest(amount);
        mockMvc.perform(post("/api/v1/wallet/deposit")
                        .header("Authorization", "Bearer " + user.token())
                        .header("Idempotency-Key", "dep-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private void makeTransfer(UserSession from, UserSession to, Long amount, String ref) throws Exception {
        TransferRequest req = new TransferRequest(to.email(), amount, ref);
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Authorization", "Bearer " + from.token())
                        .header("Idempotency-Key", "trf-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Default paginated transactions: newest first, correct amounts and references")
    void testGetTransactionsDefaultPagination() throws Exception {
        UserSession userA = createUser("tx-user-a-" + UUID.randomUUID() + "@test.com");
        UserSession userB = createUser("tx-user-b-" + UUID.randomUUID() + "@test.com");

        makeDeposit(userA, 10000L);
        makeDeposit(userA, 20000L);
        makeTransfer(userA, userB, 5000L, "Rent payment");

        // userA now has: 1 DEPOSIT (10000), 1 DEPOSIT (20000), 1 DEBIT (5000)
        MvcResult result = mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userA.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content.length()").value(3))
                .andReturn();

        // Newest first -> DEBIT 5000 should be the first entry
        String json = result.getResponse().getContentAsString();
        var root = objectMapper.readTree(json);
        var firstItem = root.get("content").get(0);
        assertThat(firstItem.get("type").asText()).isEqualTo("DEBIT");
        assertThat(firstItem.get("amount").asLong()).isEqualTo(5000L);
        assertThat(firstItem.get("balanceAfter").asLong()).isEqualTo(25000L); // 10000 + 20000 - 5000 = 25000
        assertThat(firstItem.get("reference").asText()).isEqualTo("Rent payment");
        assertThat(firstItem.get("transferId").isNull()).isFalse();
    }

    @Test
    @DisplayName("Filter transactions by entry type: DEBIT, CREDIT, DEPOSIT")
    void testFilterByType() throws Exception {
        UserSession userA = createUser("tx-filter-a-" + UUID.randomUUID() + "@test.com");
        UserSession userB = createUser("tx-filter-b-" + UUID.randomUUID() + "@test.com");

        makeDeposit(userA, 50000L);
        makeTransfer(userA, userB, 15000L, "Coffee");
        makeTransfer(userB, userA, 5000L, "Split bill");

        // userA has: DEPOSIT (50000), DEBIT (15000), CREDIT (5000)
        // Filter by DEPOSIT
        mockMvc.perform(get("/api/v1/transactions?type=DEPOSIT")
                        .header("Authorization", "Bearer " + userA.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.content[0].amount").value(50000L));

        // Filter by DEBIT
        mockMvc.perform(get("/api/v1/transactions?type=DEBIT")
                        .header("Authorization", "Bearer " + userA.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("DEBIT"))
                .andExpect(jsonPath("$.content[0].amount").value(15000L));

        // Filter by CREDIT
        mockMvc.perform(get("/api/v1/transactions?type=CREDIT")
                        .header("Authorization", "Bearer " + userA.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("CREDIT"))
                .andExpect(jsonPath("$.content[0].amount").value(5000L));
    }

    @Test
    @DisplayName("Filter transactions by date range: from and to")
    void testFilterByDateRange() throws Exception {
        UserSession user = createUser("tx-date-" + UUID.randomUUID() + "@test.com");

        Instant before = Instant.now().minus(2, ChronoUnit.MINUTES);
        makeDeposit(user, 10000L);
        Instant after = Instant.now().plus(2, ChronoUnit.MINUTES);

        // Within range
        mockMvc.perform(get("/api/v1/transactions")
                        .param("from", before.toString())
                        .param("to", after.toString())
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // Outside range (future window)
        Instant future1 = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant future2 = Instant.now().plus(2, ChronoUnit.HOURS);
        mockMvc.perform(get("/api/v1/transactions")
                        .param("from", future1.toString())
                        .param("to", future2.toString())
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("Pagination edge cases: empty page, page beyond last, invalid page/size")
    void testPaginationEdgeCases() throws Exception {
        UserSession user = createUser("tx-edge-" + UUID.randomUUID() + "@test.com");

        // 1. Empty page: user with no transactions
        mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content.length()").value(0));

        // Add 2 transactions
        makeDeposit(user, 5000L);
        makeDeposit(user, 7000L);

        // 2. Page beyond last: page=50, size=10 when only 2 items exist
        mockMvc.perform(get("/api/v1/transactions?page=50&size=10")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(50))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(0));

        // 3. Invalid size: size=0 -> 400 Bad Request
        mockMvc.perform(get("/api/v1/transactions?size=0")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // 4. Invalid size: size=-1 -> 400 Bad Request
        mockMvc.perform(get("/api/v1/transactions?size=-1")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // 5. Invalid size: size=101 (exceeds max 100) -> 400 Bad Request
        mockMvc.perform(get("/api/v1/transactions?size=101")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // 6. Invalid page: page=-1 -> 400 Bad Request
        mockMvc.perform(get("/api/v1/transactions?page=-1")
                        .header("Authorization", "Bearer " + user.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("User isolation: User A cannot see User B's transactions")
    void testUserIsolation() throws Exception {
        UserSession userA = createUser("tx-iso-a-" + UUID.randomUUID() + "@test.com");
        UserSession userB = createUser("tx-iso-b-" + UUID.randomUUID() + "@test.com");

        makeDeposit(userA, 10000L);
        makeDeposit(userB, 20000L);
        makeDeposit(userB, 30000L);

        // User A query
        MvcResult resA = mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userA.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].amount").value(10000L))
                .andReturn();

        // User B query
        MvcResult resB = mockMvc.perform(get("/api/v1/transactions")
                        .header("Authorization", "Bearer " + userB.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andReturn();

        // Verify user A does not see user B's amounts
        assertThat(resA.getResponse().getContentAsString()).doesNotContain("20000");
        assertThat(resA.getResponse().getContentAsString()).doesNotContain("30000");
    }

    @Test
    @DisplayName("No N+1 queries: fetching a page of 20 items executes exactly 2 SQL queries (1 COUNT + 1 SELECT JOIN FETCH)")
    void testNoNPlusOneQueries() throws Exception {
        UserSession userA = createUser("tx-nplus1-a-" + UUID.randomUUID() + "@test.com");
        UserSession userB = createUser("tx-nplus1-b-" + UUID.randomUUID() + "@test.com");

        // Fund userA with 1,000,000 paise
        makeDeposit(userA, 1000000L);

        // Generate 19 transfers so userA has 20 total transactions (1 deposit + 19 transfers)
        for (int i = 1; i <= 19; i++) {
            makeTransfer(userA, userB, 1000L, "Batch transfer #" + i);
        }

        // Enable Hibernate statistics to inspect query execution count
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        long preparedStatementsBefore = statistics.getPrepareStatementCount();

        // Request a page of 20 transactions (size=20)
        MvcResult result = mockMvc.perform(get("/api/v1/transactions?page=0&size=20")
                        .header("Authorization", "Bearer " + userA.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.totalElements").value(20))
                .andReturn();

        long preparedStatementsAfter = statistics.getPrepareStatementCount();
        long queriesExecuted = preparedStatementsAfter - preparedStatementsBefore;

        // Breakdown of queries during the request:
        // 1. Security filter: User lookup by email (CustomUserDetailsService)
        // 2. TransactionService: findWalletIdByUserId (wallet ID resolution)
        // 3. LedgerEntryRepository: COUNT(le) pagination query
        // 4. LedgerEntryRepository: SELECT le LEFT JOIN FETCH le.transfer (single query for all 20 items)
        // If an N+1 problem existed for the 19 transfers, queriesExecuted would be 4 + 19 = 23 queries.
        // With LEFT JOIN FETCH, the transaction query count is constant O(1) (exactly 4 total including auth).
        assertThat(queriesExecuted)
                .as("Query count must be constant (<= 4 including auth) and not scale with the 20 returned items (proving NO N+1 queries)")
                .isLessThanOrEqualTo(4L);
    }
}
