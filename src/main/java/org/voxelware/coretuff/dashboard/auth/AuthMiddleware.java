package org.voxelware.coretuff.dashboard.auth;

import com.sun.net.httpserver.HttpExchange;

import org.voxelware.coretuff.dashboard.DashboardConfig;
import org.voxelware.coretuff.dashboard.auth.JwtProvider.JwtClaims;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthMiddleware {

    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;
    private static final int MAX_REQUESTS_PER_WINDOW = 60;

    private final JwtProvider jwtProvider;
    private final DashboardConfig config;
    private final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();

    private record RateLimitEntry(int count, long windowStart) {}

    public record AuthResult(boolean allowed, JwtClaims claims, String errorMessage) {
        public static AuthResult authorized(JwtClaims claims) {
            return new AuthResult(true, claims, null);
        }
        public static AuthResult denied(String message) {
            return new AuthResult(false, null, message);
        }
    }

    public AuthMiddleware(JwtProvider jwtProvider, DashboardConfig config) {
        this.jwtProvider = jwtProvider;
        this.config = config;
    }

    public AuthResult authenticate(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();

        if (path.startsWith("/api/v1/pair/")) {
            return AuthResult.authorized(null);
        }
        if (path.equals("/api/v1/health")) {
            return AuthResult.authorized(null);
        }

        String ip = getClientIp(exchange);
        RateLimitResult rateResult = checkRateLimit(ip);
        if (!rateResult.allowed()) {
            return AuthResult.denied("Rate limit exceeded. Try again in " + rateResult.retryAfterMs() + "ms");
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return AuthResult.denied("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7).trim();
        JwtClaims claims = jwtProvider.verify(token);
        if (claims == null) {
            return AuthResult.denied("Invalid or expired JWT token");
        }

        return AuthResult.authorized(claims);
    }

    private record RateLimitResult(boolean allowed, long retryAfterMs) {}

    private RateLimitResult checkRateLimit(String ip) {
        long now = System.currentTimeMillis();
        RateLimitEntry entry = rateLimitMap.compute(ip, (key, existing) -> {
            if (existing == null || (now - existing.windowStart()) > RATE_LIMIT_WINDOW_MS) {
                return new RateLimitEntry(1, now);
            }
            return new RateLimitEntry(existing.count() + 1, existing.windowStart());
        });
        if (entry.count() > MAX_REQUESTS_PER_WINDOW) {
            long retryAfter = RATE_LIMIT_WINDOW_MS - (now - entry.windowStart());
            return new RateLimitResult(false, Math.max(retryAfter, 0));
        }
        return new RateLimitResult(true, 0);
    }

    private String getClientIp(HttpExchange exchange) {
        String xff = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        var addr = exchange.getRemoteAddress();
        return addr != null ? addr.getHostString() : "unknown";
    }
}
