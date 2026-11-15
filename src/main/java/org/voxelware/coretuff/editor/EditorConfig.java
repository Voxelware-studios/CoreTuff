package org.voxelware.coretuff.editor;

import org.voxelware.coretuff.database.config.ConfigurationService;
import org.voxelware.coretuff.database.config.DatabaseConfigProvider;

import java.util.UUID;

public final class EditorConfig {

    private static final String SERVER_ID_KEY = "editor.server-id";

    private final DatabaseConfigProvider db;
    private String serverId;
    private boolean devMode = false;
    private ConfigurationService configService;

    public EditorConfig() {
        this.db = new DatabaseConfigProvider("editor");
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public void setConfigurationService(ConfigurationService configService) {
        this.configService = configService;
        db.setConfigurationService(configService);
        reload();
    }

    public void reload() {
        db.reload();
        this.serverId = loadOrCreateServerId();
    }

    private String loadOrCreateServerId() {
        String existing = db.getString(SERVER_ID_KEY, null);
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }
        String newId = UUID.randomUUID().toString();
        db.set(SERVER_ID_KEY, newId);
        return newId;
    }

    public boolean isEnabled() {
        return db.getBoolean("editor.enabled", true);
    }

    public String getBackendUrl() {
        if (devMode) {
            return "http://localhost:25570";
        }
        return db.getString("editor.backend-url", "https://ct-backend-b8cy.onrender.com").replaceAll("/$", "");
    }

    public String getFrontendUrl() {
        if (devMode) {
            return "http://localhost:5173";
        }
        return db.getString("editor.frontend-url", "https://ct.voxelware.org").replaceAll("/$", "");
    }

    public int getSessionExpiryMinutes() {
        return db.getInt("editor.session-expiry-minutes", 15);
    }

    public String getServerId() {
        return serverId;
    }
}
