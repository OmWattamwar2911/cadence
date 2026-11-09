package com.walletservice.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private final long authCapacity;
    private final long authRefillTokens;
    private final long authRefillDurationSeconds;

    private final long transferCapacity;
    private final long transferRefillTokens;
    private final long transferRefillDurationSeconds;

    private final ConcurrentHashMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> transferBuckets = new ConcurrentHashMap<>();

    public RateLimitingService(
            @Value("${app.rate-limit.auth.capacity:${app.rate-limit.login.capacity:5}}") long authCapacity,
            @Value("${app.rate-limit.auth.refill-tokens:${app.rate-limit.login.refill-tokens:5}}") long authRefillTokens,
            @Value("${app.rate-limit.auth.refill-duration-seconds:${app.rate-limit.login.refill-duration-seconds:60}}") long authRefillDurationSeconds,
            @Value("${app.rate-limit.transfer.capacity:10}") long transferCapacity,
            @Value("${app.rate-limit.transfer.refill-tokens:10}") long transferRefillTokens,
            @Value("${app.rate-limit.transfer.refill-duration-seconds:60}") long transferRefillDurationSeconds) {
        this.authCapacity = authCapacity;
        this.authRefillTokens = authRefillTokens;
        this.authRefillDurationSeconds = authRefillDurationSeconds;
        this.transferCapacity = transferCapacity;
        this.transferRefillTokens = transferRefillTokens;
        this.transferRefillDurationSeconds = transferRefillDurationSeconds;
    }

    public ConsumptionProbe tryConsumeAuth(String clientIp) {
        Bucket bucket = authBuckets.computeIfAbsent(clientIp, k -> createBucket(authCapacity, authRefillTokens, authRefillDurationSeconds));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public ConsumptionProbe tryConsumeTransfer(String clientKey) {
        Bucket bucket = transferBuckets.computeIfAbsent(clientKey, k -> createBucket(transferCapacity, transferRefillTokens, transferRefillDurationSeconds));
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public void clearBuckets() {
        authBuckets.clear();
        transferBuckets.clear();
    }

    public long getAuthCapacity() {
        return authCapacity;
    }

    public long getTransferCapacity() {
        return transferCapacity;
    }

    private Bucket createBucket(long capacity, long refillTokens, long durationSeconds) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, Duration.ofSeconds(durationSeconds))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
