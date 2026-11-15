package org.voxelware.coretuff.dashboard;

import org.voxelware.coretuff.database.config.ConfigurationService;
import org.voxelware.coretuff.database.config.DatabaseConfigProvider;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

public final class DashboardConfig {

    private final DatabaseConfigProvider db;
    private ConfigurationService configService;

    public DashboardConfig() {
        this.db = new DatabaseConfigProvider("dashboard");
    }

    public void setConfigurationService(ConfigurationService configService) {
        this.configService = configService;
        db.setConfigurationService(configService);
        reload();
    }

    public void reload() {
        db.reload();
    }

    public boolean isEnabled() {
        return db.getBoolean("dashboard.enabled", false);
    }

    public int getPort() {
        return db.getInt("dashboard.port", 4567);
    }

    public String getBindAddress() {
        return db.getString("dashboard.bind-address", "0.0.0.0");
    }

    public int getMaxPairSessions() {
        return db.getInt("dashboard.max-pair-sessions", 10);
    }

    public int getPairCodeExpiryMinutes() {
        return db.getInt("dashboard.pair-code-expiry-minutes", 5);
    }

    public String getJwtSecret() {
        String secret = db.getString("dashboard.jwt-secret", null);
        if (secret == null || secret.isEmpty()) {
            secret = generateSecret();
            if (configService != null) {
                configService.set("dashboard", "jwt-secret", secret);
                db.reload();
            }
        }
        return secret;
    }

    public int getJwtExpiryHours() {
        return db.getInt("dashboard.jwt-expiry-hours", 24);
    }

    public List<String> getAllowedOrigins() {
        var raw = db.getString("dashboard.allowed-origins", null);
        if (raw == null || raw.isEmpty()) {
            return List.of("*");
        }
        if (raw.startsWith("[")) {
            try {
                var arr = com.google.gson.JsonParser.parseString(raw).getAsJsonArray();
                java.util.ArrayList<String> result = new java.util.ArrayList<>();
                for (var el : arr) result.add(el.getAsString());
                return result;
            } catch (Exception ignored) {}
        }
        return List.of(raw.split(","));
    }

    public String getEditorBackendUrl() {
        return db.getString("dashboard.editor-backend-url", "http://localhost:25570");
    }

    public int getEditorSessionExpiryMinutes() {
        return db.getInt("dashboard.editor-session-expiry-minutes", 15);
    }

    private String generateSecret() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
