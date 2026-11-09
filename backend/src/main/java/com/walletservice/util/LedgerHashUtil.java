package com.walletservice.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class LedgerHashUtil {

    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private LedgerHashUtil() {}

    public static String calculateHash(String prevHash, Long walletId, String entryType, Long amount, Long balanceAfter) {
        String safePrev = (prevHash != null && !prevHash.isBlank()) ? prevHash : GENESIS_HASH;
        String raw = safePrev + ":" + walletId + ":" + entryType + ":" + amount + ":" + balanceAfter;
        return sha256Hex(raw);
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
