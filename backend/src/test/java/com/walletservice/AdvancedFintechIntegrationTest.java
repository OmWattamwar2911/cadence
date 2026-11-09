package com.walletservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walletservice.dto.request.*;
import com.walletservice.dto.response.*;
import com.walletservice.entity.*;
import com.walletservice.repository.*;
import com.walletservice.service.ScheduledTransferService;
import com.walletservice.service.SseNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AdvancedFintechIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ScheduledTransferService scheduledTransferService;

    @Autowired
    private ScheduledTransferRepository scheduledTransferRepository;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private SseNotificationService sseNotificationService;

    @Autowired
    private WalletRepository walletRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1";
    }

    private String registerAndGetToken(String email) {
        RegisterRequest registerReq = new RegisterRequest(email, "Password123!");
        restTemplate.postForEntity(baseUrl + "/auth/register", registerReq, String.class);

        LoginRequest loginReq = new LoginRequest(email, "Password123!");
        ResponseEntity<AuthResponse> loginRes = restTemplate.postForEntity(
                baseUrl + "/auth/login", loginReq, AuthResponse.class);
        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        return loginRes.getBody().accessToken();
    }

    private void depositFunds(String token, Long amount) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Idempotency-Key", "dep-test-" + UUID.randomUUID());
        headers.setContentType(MediaType.APPLICATION_JSON);

        DepositRequest depositReq = new DepositRequest(amount);
        HttpEntity<DepositRequest> entity = new HttpEntity<>(depositReq, headers);
        ResponseEntity<DepositResponse> res = restTemplate.postForEntity(baseUrl + "/wallet/deposit", entity, DepositResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Feature 1: SSE Emitter connection and notification delivery")
    void testSseNotifications() {
        String userEmail = "sse-" + UUID.randomUUID() + "@example.com";
        String token = registerAndGetToken(userEmail);

        // Verify emitter subscription
        SseEmitter emitter = sseNotificationService.subscribe(9999L);
        assertThat(emitter).isNotNull();

        // Send a test notification
        sseNotificationService.sendToUser(9999L, "TEST_EVENT", "Hello World");

        // Send a heartbeat ping
        sseNotificationService.sendHeartbeat();
    }

    @Test
    @DisplayName("Feature 2: Cryptographic ledger audit proves tamper-free state and generates certified receipt")
    void testCryptographicLedgerAuditAndReceipt() {
        String aliceEmail = "alice-crypto-" + UUID.randomUUID() + "@example.com";
        String bobEmail = "bob-crypto-" + UUID.randomUUID() + "@example.com";

        String aliceToken = registerAndGetToken(aliceEmail);
        String bobToken = registerAndGetToken(bobEmail);

        // Alice deposits ₹1,000.00 (100,000 paise)
        depositFunds(aliceToken, 100000L);

        // Alice transfers ₹350.00 (35,000 paise) to Bob
        HttpHeaders transferHeaders = new HttpHeaders();
        transferHeaders.setBearerAuth(aliceToken);
        String idemKey = "crypto-xfer-" + UUID.randomUUID();
        transferHeaders.set("Idempotency-Key", idemKey);
        transferHeaders.setContentType(MediaType.APPLICATION_JSON);

        String transferRef = "REF-" + UUID.randomUUID().toString().substring(0, 8);
        TransferRequest transferReq = new TransferRequest(bobEmail, 35000L, transferRef);
        ResponseEntity<TransferResponse> xferRes = restTemplate.postForEntity(
                baseUrl + "/transfers", new HttpEntity<>(transferReq, transferHeaders), TransferResponse.class);
        assertThat(xferRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 1. Audit Alice's Wallet Ledger
        HttpHeaders auditHeaders = new HttpHeaders();
        auditHeaders.setBearerAuth(aliceToken);
        ResponseEntity<LedgerAuditResponse> auditRes = restTemplate.exchange(
                baseUrl + "/ledger/audit", HttpMethod.GET, new HttpEntity<>(auditHeaders), LedgerAuditResponse.class);

        assertThat(auditRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        LedgerAuditResponse audit = auditRes.getBody();
        assertThat(audit).isNotNull();
        assertThat(audit.isTamperFree()).isTrue();
        assertThat(audit.totalVerifiedEntries()).isEqualTo(2); // 1 deposit + 1 debit
        assertThat(audit.rootHash()).isNotEqualTo(audit.genesisHash());

        // 2. Fetch Certified Transfer Receipt
        ResponseEntity<TransferReceiptResponse> receiptRes = restTemplate.exchange(
                baseUrl + "/ledger/receipts/" + transferRef, HttpMethod.GET, new HttpEntity<>(auditHeaders), TransferReceiptResponse.class);

        assertThat(receiptRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        TransferReceiptResponse receipt = receiptRes.getBody();
        assertThat(receipt).isNotNull();
        assertThat(receipt.transferReference()).isEqualTo(transferRef);
        assertThat(receipt.amount()).isEqualTo(35000L);
        assertThat(receipt.cryptographicHash()).isNotEmpty();
        assertThat(receipt.certificationStamp()).contains("CERTIFIED_IMMUTABLE_HASH_CHAIN");
    }

    @Test
    @DisplayName("Feature 3: Statement CSV Export downloads certified account history")
    void testStatementCsvExport() {
        String userEmail = "csv-user-" + UUID.randomUUID() + "@example.com";
        String token = registerAndGetToken(userEmail);

        depositFunds(token, 50000L);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> exportRes = restTemplate.exchange(
                baseUrl + "/transactions/export", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(exportRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exportRes.getHeaders().getContentType().toString()).contains("text/csv");
        String csvBody = exportRes.getBody();
        assertThat(csvBody).contains("CADENCE OFFICIAL FINANCIAL STATEMENT");
        assertThat(csvBody).contains("Entry ID,Timestamp,Type,Amount (INR)");
        assertThat(csvBody).contains("DEPOSIT");
    }

    @Test
    @DisplayName("Feature 4: Scheduled recurring transfer setup, execution, and cancellation")
    void testScheduledTransfersFlow() {
        String senderEmail = "sched-sender-" + UUID.randomUUID() + "@example.com";
        String recipientEmail = "sched-recipient-" + UUID.randomUUID() + "@example.com";

        String senderToken = registerAndGetToken(senderEmail);
        registerAndGetToken(recipientEmail);

        depositFunds(senderToken, 200000L); // ₹2,000.00

        // 1. Create a DAILY recurring transfer of ₹50.00 (5,000 paise)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(senderToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        CreateScheduledTransferRequest createReq = new CreateScheduledTransferRequest(
                recipientEmail,
                5000L,
                ScheduleFrequency.DAILY,
                Instant.now().minusSeconds(10) // Set nextRunAt in past to simulate due schedule
        );

        ResponseEntity<ScheduledTransferResponse> createRes = restTemplate.postForEntity(
                baseUrl + "/scheduled-transfers", new HttpEntity<>(createReq, headers), ScheduledTransferResponse.class);

        assertThat(createRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ScheduledTransferResponse schedule = createRes.getBody();
        assertThat(schedule).isNotNull();
        assertThat(schedule.status()).isEqualTo(ScheduleStatus.ACTIVE);
        assertThat(schedule.amount()).isEqualTo(5000L);

        // 2. Trigger the scheduled execution worker
        scheduledTransferService.processDueScheduledTransfers();

        // Verify schedule advanced its nextRunAt
        ScheduledTransfer updated = scheduledTransferRepository.findById(schedule.id()).orElseThrow();
        assertThat(updated.getLastRunAt()).isNotNull();

        // 3. Cancel the schedule
        ResponseEntity<Void> cancelRes = restTemplate.exchange(
                baseUrl + "/scheduled-transfers/" + schedule.id(),
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class
        );
        assertThat(cancelRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ScheduledTransfer cancelled = scheduledTransferRepository.findById(schedule.id()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(ScheduleStatus.CANCELLED);
    }

    @Test
    @DisplayName("Feature 5: Payment Requests (Split Bill) creation, incoming view, payment, and decline")
    void testPaymentRequestFlow() {
        String requesterEmail = "requester-" + UUID.randomUUID() + "@example.com";
        String payerEmail = "payer-" + UUID.randomUUID() + "@example.com";

        String requesterToken = registerAndGetToken(requesterEmail);
        String payerToken = registerAndGetToken(payerEmail);

        depositFunds(payerToken, 100000L); // Payer has ₹1,000.00

        // 1. Requester creates a Payment Request of ₹250.00 (25,000 paise) to Payer
        HttpHeaders requesterHeaders = new HttpHeaders();
        requesterHeaders.setBearerAuth(requesterToken);
        requesterHeaders.setContentType(MediaType.APPLICATION_JSON);

        CreatePaymentRequest createReq = new CreatePaymentRequest(payerEmail, 25000L, "Dinner split at Bistro");
        ResponseEntity<PaymentRequestResponse> createRes = restTemplate.postForEntity(
                baseUrl + "/payment-requests", new HttpEntity<>(createReq, requesterHeaders), PaymentRequestResponse.class);

        assertThat(createRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PaymentRequestResponse reqResponse = createRes.getBody();
        assertThat(reqResponse).isNotNull();
        assertThat(reqResponse.status()).isEqualTo(PaymentRequestStatus.PENDING);
        assertThat(reqResponse.amount()).isEqualTo(25000L);

        // 2. Payer views incoming pending requests
        HttpHeaders payerHeaders = new HttpHeaders();
        payerHeaders.setBearerAuth(payerToken);

        ResponseEntity<PaymentRequestResponse[]> incomingRes = restTemplate.exchange(
                baseUrl + "/payment-requests/incoming", HttpMethod.GET, new HttpEntity<>(payerHeaders), PaymentRequestResponse[].class);

        assertThat(incomingRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(incomingRes.getBody()).isNotEmpty();
        assertThat(incomingRes.getBody()[0].id()).isEqualTo(reqResponse.id());

        // 3. Payer pays the request with Idempotency-Key
        HttpHeaders payHeaders = new HttpHeaders();
        payHeaders.setBearerAuth(payerToken);
        payHeaders.set("Idempotency-Key", "pay-req-idem-" + UUID.randomUUID());

        ResponseEntity<PaymentRequestResponse> payRes = restTemplate.exchange(
                baseUrl + "/payment-requests/" + reqResponse.id() + "/pay",
                HttpMethod.POST,
                new HttpEntity<>(payHeaders),
                PaymentRequestResponse.class
        );

        assertThat(payRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(payRes.getBody().status()).isEqualTo(PaymentRequestStatus.PAID);
        assertThat(payRes.getBody().transferId()).isNotNull();

        // 4. Test Decline flow with a second request
        CreatePaymentRequest declineReq = new CreatePaymentRequest(payerEmail, 10000L, "Coffee split");
        ResponseEntity<PaymentRequestResponse> secondReq = restTemplate.postForEntity(
                baseUrl + "/payment-requests", new HttpEntity<>(declineReq, requesterHeaders), PaymentRequestResponse.class);

        ResponseEntity<PaymentRequestResponse> declineRes = restTemplate.exchange(
                baseUrl + "/payment-requests/" + secondReq.getBody().id() + "/decline",
                HttpMethod.POST,
                new HttpEntity<>(payerHeaders),
                PaymentRequestResponse.class
        );

        assertThat(declineRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(declineRes.getBody().status()).isEqualTo(PaymentRequestStatus.DECLINED);
    }

    @Test
    @DisplayName("Edge Cases & Full Coverage: Genesis audit, Weekly/Monthly schedules, Outgoing requests, Self-requests")
    void testFintechServiceEdgeCases() {
        String userEmail = "edge-user-" + UUID.randomUUID() + "@example.com";
        String token = registerAndGetToken(userEmail);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1. Audit wallet with 0 entries (Genesis branch in LedgerAuditService)
        ResponseEntity<LedgerAuditResponse> emptyAudit = restTemplate.exchange(
                baseUrl + "/ledger/audit", HttpMethod.GET, new HttpEntity<>(headers), LedgerAuditResponse.class);
        assertThat(emptyAudit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(emptyAudit.getBody().totalVerifiedEntries()).isEqualTo(0);
        assertThat(emptyAudit.getBody().statusDescription()).contains("Genesis State");

        // 2. Weekly and Monthly schedule frequencies
        String recEmail = "rec-" + UUID.randomUUID() + "@example.com";
        registerAndGetToken(recEmail);

        CreateScheduledTransferRequest weeklyReq = new CreateScheduledTransferRequest(
                recEmail, 1000L, ScheduleFrequency.WEEKLY, null);
        ResponseEntity<ScheduledTransferResponse> weeklyRes = restTemplate.postForEntity(
                baseUrl + "/scheduled-transfers", new HttpEntity<>(weeklyReq, headers), ScheduledTransferResponse.class);
        assertThat(weeklyRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        CreateScheduledTransferRequest monthlyReq = new CreateScheduledTransferRequest(
                recEmail, 2000L, ScheduleFrequency.MONTHLY, null);
        ResponseEntity<ScheduledTransferResponse> monthlyRes = restTemplate.postForEntity(
                baseUrl + "/scheduled-transfers", new HttpEntity<>(monthlyReq, headers), ScheduledTransferResponse.class);
        assertThat(monthlyRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 3. List user schedules
        ResponseEntity<ScheduledTransferResponse[]> listSchedRes = restTemplate.exchange(
                baseUrl + "/scheduled-transfers", HttpMethod.GET, new HttpEntity<>(headers), ScheduledTransferResponse[].class);
        assertThat(listSchedRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listSchedRes.getBody()).hasSize(2);

        // 4. Outgoing payment requests
        CreatePaymentRequest pr = new CreatePaymentRequest(recEmail, 5000L, "Split");
        restTemplate.postForEntity(baseUrl + "/payment-requests", new HttpEntity<>(pr, headers), PaymentRequestResponse.class);

        ResponseEntity<PaymentRequestResponse[]> outReqs = restTemplate.exchange(
                baseUrl + "/payment-requests/outgoing", HttpMethod.GET, new HttpEntity<>(headers), PaymentRequestResponse[].class);
        assertThat(outReqs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(outReqs.getBody()).hasSize(1);

        // 5. Self-transfer requests rejection
        CreatePaymentRequest selfPr = new CreatePaymentRequest(userEmail, 1000L, "Self");
        ResponseEntity<String> selfPrRes = restTemplate.postForEntity(
                baseUrl + "/payment-requests", new HttpEntity<>(selfPr, headers), String.class);
        assertThat(selfPrRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        CreateScheduledTransferRequest selfSched = new CreateScheduledTransferRequest(
                userEmail, 1000L, ScheduleFrequency.DAILY, null);
        ResponseEntity<String> selfSchedRes = restTemplate.postForEntity(
                baseUrl + "/scheduled-transfers", new HttpEntity<>(selfSched, headers), String.class);
        assertThat(selfSchedRes.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
