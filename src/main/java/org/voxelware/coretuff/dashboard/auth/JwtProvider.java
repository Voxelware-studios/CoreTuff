package org.voxelware.coretuff.dashboard.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JwtProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String HEADER_BASE64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(HEADER.getBytes(StandardCharsets.UTF_8));

    private final byte[] secretBytes;
    private final String secretBase64;

    public JwtProvider(String secret) {
        this.secretBase64 = secret;
        this.secretBytes = Base64.getDecoder().decode(secret);
    }

    public record JwtClaims(String serverUuid, String name, long expiry, long issuedAt) {}

    public String generateToken(String serverUuid, String name, long expiryHours) {
        long issuedAt = System.currentTimeMillis() / 1000;
        long expiry = issuedAt + (expiryHours * 3600);
        String payload = """
                {
                  "server_uuid": "%s",
                  "name": "%s",
                  "iat": %d,
                  "exp": %d
                }""".formatted(serverUuid, name, issuedAt, expiry);
        String payloadBase64 = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(HEADER_BASE64 + "." + payloadBase64);
        return HEADER_BASE64 + "." + payloadBase64 + "." + signature;
    }

    public JwtClaims verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String headerB64 = parts[0];
            String payloadB64 = parts[1];
            String signatureB64 = parts[2];

            String expectedSig = sign(headerB64 + "." + payloadB64);
            if (!constantTimeEquals(signatureB64, expectedSig)) return null;

            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadB64);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);

            long expiry = extractLong(payload, "exp");
            if (System.currentTimeMillis() / 1000 > expiry) return null;

            String serverUuid = extractString(payload, "server_uuid");
            String name = extractString(payload, "name");
            long issuedAt = extractLong(payload, "iat");

            return new JwtClaims(serverUuid, name, expiry, issuedAt);
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            byte[] sigBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sigBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign JWT", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private String extractString(String json, String key) {
        String search = "\"%s\"".formatted(key);
        int idx = json.indexOf(search);
        if (idx == -1) return "";
        int start = json.indexOf('"', idx + search.length() + 1);
        if (start == -1) return "";
        int end = json.indexOf('"', start + 1);
        return end == -1 ? "" : json.substring(start + 1, end);
    }

    private long extractLong(String json, String key) {
        String search = "\"%s\"".formatted(key);
        int idx = json.indexOf(search);
        if (idx == -1) return 0;
        int colon = json.indexOf(':', idx);
        if (colon == -1) return 0;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try {
            return Long.parseLong(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
