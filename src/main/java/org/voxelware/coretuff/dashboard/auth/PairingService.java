package org.voxelware.coretuff.dashboard.auth;

import org.voxelware.coretuff.dashboard.DashboardConfig;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PairingService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtProvider jwtProvider;
    private final DashboardConfig config;
    private final Map<String, PairSession> sessions = new ConcurrentHashMap<>();

    public record PairSession(String code, long createdAt, long expiresAt, String creatorIp, String serverName, String serverUuid) {}
    public record ExchangeResult(String token, String serverName, String serverUuid, int expiresInHours) {}

    public PairingService(JwtProvider jwtProvider, DashboardConfig config) {
        this.jwtProvider = jwtProvider;
        this.config = config;
    }

    public String generateCode(String creatorIp, String serverName, String serverUuid) {
        cleanup();
        if (sessions.size() >= config.getMaxPairSessions()) {
            return null;
        }
        String code;
        do {
            code = generateRandomCode();
        } while (sessions.containsKey(code));

        long now = System.currentTimeMillis();
        long expiryMs = config.getPairCodeExpiryMinutes() * 60_000L;
        sessions.put(code, new PairSession(code, now, now + expiryMs, creatorIp, serverName, serverUuid));
        return code;
    }

    public ExchangeResult exchangeCode(String code, String requestIp) {
        PairSession session = sessions.get(code);
        if (session == null) return null;
        if (System.currentTimeMillis() > session.expiresAt()) {
            sessions.remove(code);
            return null;
        }
        sessions.remove(code);
        String token = jwtProvider.generateToken(
                session.serverUuid(),
                session.serverName(),
                config.getJwtExpiryHours()
        );
        return new ExchangeResult(token, session.serverName(), session.serverUuid(), config.getJwtExpiryHours());
    }

    public boolean isValidCode(String code) {
        PairSession session = sessions.get(code);
        if (session == null) return false;
        if (System.currentTimeMillis() > session.expiresAt()) {
            sessions.remove(code);
            return false;
        }
        return true;
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        sessions.values().removeIf(s -> now > s.expiresAt());
    }

    public int getActiveSessionCount() {
        cleanup();
        return sessions.size();
    }

    private String generateRandomCode() {
        var sb = new StringBuilder(19);
        for (int i = 0; i < 19; i++) {
            if (i == 4 || i == 9 || i == 14) sb.append('-');
            else sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
