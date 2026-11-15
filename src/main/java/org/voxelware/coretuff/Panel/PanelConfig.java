package org.voxelware.coretuff.Panel;

import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.CoreTuff;

import java.util.UUID;

public final class PanelConfig {

	public static final String DEFAULT_BACKEND_URL = "https://ct-backend-b8cy.onrender.com";

	private final CoreTuff plugin;

	private boolean enabled;
	private String backendUrl;
	private int reportIntervalSeconds;
	private int heartbeatIntervalSeconds;
	private String serverId;

	public PanelConfig(CoreTuff plugin) {
		this.plugin = plugin;
		reload();
	}

	public void reload() {
		FileConfiguration cfg = plugin.getCoreConfigLoader().getConfig();
		this.enabled = cfg.getBoolean("Panel.Enabled", true);
		this.backendUrl = firstNonBlank(cfg.getString("Panel.Backend-Url"), DEFAULT_BACKEND_URL);
		this.reportIntervalSeconds = clamp(cfg.getInt("Panel.Report-Interval-Seconds", 60), 10, 3600);
		this.heartbeatIntervalSeconds = clamp(cfg.getInt("Panel.Heartbeat-Interval-Seconds", 10), 5, 60);
		this.serverId = this.enabled ? ensureServerId(cfg) : firstNonBlank(cfg.getString("Panel.Server-Id"), "");
	}

	private String ensureServerId(FileConfiguration cfg) {
		String existing = firstNonBlank(cfg.getString("Panel.Server-Id"), "");
		if (!existing.isEmpty()) {
			return existing;
		}
		String generated = UUID.randomUUID().toString();
		cfg.set("Panel.Server-Id", generated);
		plugin.getCoreConfigLoader().save();
		plugin.getLogger().info("Generated panel server id: " + generated);
		return generated;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String getBackendUrl() {
		return backendUrl;
	}

	public String getWebSocketUrl() {
		String url = backendUrl.replaceAll("/+$", "");
		if (url.startsWith("http://")) {
			return "ws://" + url.substring("http://".length()) + "/ws";
		}
		if (url.startsWith("https://")) {
			return "wss://" + url.substring("https://".length()) + "/ws";
		}
		if (url.startsWith("ws://") || url.startsWith("wss://")) {
			return url.endsWith("/ws") ? url : url + "/ws";
		}
		return "wss://" + url + "/ws";
	}

	public int getReportIntervalSeconds() {
		return reportIntervalSeconds;
	}

	public int getHeartbeatIntervalSeconds() {
		return heartbeatIntervalSeconds;
	}

	public String getServerId() {
		return serverId;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static String firstNonBlank(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value.trim();
	}
}
