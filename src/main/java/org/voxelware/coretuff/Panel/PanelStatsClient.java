package org.voxelware.coretuff.Panel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.voxelware.coretuff.CoreTuff;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Stats-only link between CoreTuff 1.1 and the CoreTuff web panel.
 *
 * The client registers with the panel, then periodically streams a health report so the
 * server contributes to the panel's public statistics aggregates. It never opens editor
 * sessions and never issues moderation/player queries - the panel classifies this plugin
 * version as stats-only.
 */
public final class PanelStatsClient {

	private static final Gson GSON = new Gson();
	private static final int MAX_RECONNECT_DELAY_SECONDS = 60;

	private final CoreTuff plugin;
	private final PanelConfig config;
	private final PanelHealthCollector collector;
	private final AtomicBoolean rejected = new AtomicBoolean(false);

	private final Object sendLock = new Object();
	private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);

	private HttpClient httpClient;
	private ScheduledExecutorService executor;
	private WebSocket webSocket;
	private volatile boolean running;
	private volatile boolean registered;
	private int reconnectDelay = 2;

	public PanelStatsClient(CoreTuff plugin, PanelConfig config) {
		this.plugin = plugin;
		this.config = config;
		this.collector = new PanelHealthCollector(plugin);
	}

	public void start() {
		if (!config.isEnabled()) {
			plugin.getLogger().info("Panel stats link is disabled in config (Panel.Enabled=false).");
			return;
		}
		if (running) {
			return;
		}
		running = true;
		rejected.set(false);
		reconnectScheduled.set(false);
		reconnectDelay = 2;
		httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(15))
				.build();
		executor = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread thread = new Thread(r, "ct-panel-stats");
			thread.setDaemon(true);
			return thread;
		});

		executor.scheduleAtFixedRate(this::sendHeartbeat,
				config.getHeartbeatIntervalSeconds(), config.getHeartbeatIntervalSeconds(), TimeUnit.SECONDS);
		executor.scheduleAtFixedRate(this::sendHealthReport,
				config.getReportIntervalSeconds(), config.getReportIntervalSeconds(), TimeUnit.SECONDS);
		connect();
	}

	public void stop() {
		if (!running && webSocket == null && executor == null) {
			return;
		}
		running = false;
		registered = false;

		if (executor != null) {
			executor.shutdownNow();
			executor = null;
		}
		WebSocket ws = webSocket;
		webSocket = null;
		if (ws != null) {
			try {
				ws.sendClose(1000, "shutdown");
			} catch (Throwable ignored) {
			}
		}
		plugin.getLogger().info("Panel stats link stopped.");
	}

	public boolean isRunning() {
		return running;
	}

	private void connect() {
		if (!running || rejected.get() || webSocket != null) {
			return;
		}
		HttpClient client = httpClient;
		if (client == null) {
			return;
		}
		client.newWebSocketBuilder()
				.connectTimeout(Duration.ofSeconds(15))
				.buildAsync(URI.create(config.getWebSocketUrl()), new Listener())
				.thenAccept(ws -> {
					reconnectDelay = 2;
					plugin.getLogger().info("Connected to panel backend (stats-only).");
				})
				.exceptionally(ex -> {
					plugin.getLogger().warning("Panel stats link failed to connect: " + rootMessage(ex));
					scheduleReconnect();
					return null;
				});
	}

	private void onSocketOpen(WebSocket ws) {
		this.webSocket = ws;
		this.registered = false;
		JsonObject register = new JsonObject();
		register.addProperty("type", "register");
		register.addProperty("server_id", config.getServerId());
		register.addProperty("server_name", plugin.getServer().getName());
		register.addProperty("plugin_version", plugin.getDescription().getVersion());
		send(register);
	}

	private void onSocketClosed(String reason) {
		this.webSocket = null;
		this.registered = false;
		if (running && !rejected.get()) {
			plugin.getLogger().info("Panel stats link closed (" + reason + ").");
			scheduleReconnect();
		}
	}

	private void scheduleReconnect() {
		ScheduledExecutorService scheduledExecutor = executor;
		if (!running || rejected.get() || scheduledExecutor == null || scheduledExecutor.isShutdown()) {
			return;
		}
		// onError and onClose can both fire for the same socket - never queue twice.
		if (!reconnectScheduled.compareAndSet(false, true)) {
			return;
		}
		plugin.getLogger().info("Reconnecting to panel backend in " + reconnectDelay + "s...");
		scheduledExecutor.schedule(() -> {
			reconnectScheduled.set(false);
			connect();
		}, reconnectDelay, TimeUnit.SECONDS);
		reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_DELAY_SECONDS);
	}

	private void handleServerMessage(String text) {
		if (text == null || text.isBlank()) {
			return;
		}
		try {
			JsonObject message = JsonParser.parseString(text).getAsJsonObject();
			String type = message.has("type") ? message.get("type").getAsString() : "";
			switch (type) {
				case "register_ack" -> {
					registered = true;
					String mode = message.has("mode") ? message.get("mode").getAsString() : "stats";
					plugin.getLogger().info(
							"Panel accepted this server (mode: " + mode + ", panel stats reporting active).");
				}
				case "register_rejected" -> {
					rejected.set(true);
					String panel = message.has("panel_version") ? message.get("panel_version").getAsString() : "unknown";
					plugin.getLogger().severe("Panel rejected this server. Panel version: " + panel);
					plugin.getLogger().severe("Panel stats reporting disabled - no further retries will be made.");
					WebSocket ws = webSocket;
					if (ws != null) {
						try {
							ws.sendClose(1000, "unsupported plugin version");
						} catch (Throwable ignored) {
						}
					}
				}
				case "error" -> plugin.getLogger().warning(
						"Panel reported an error: " + (message.has("message") ? message.get("message").getAsString() : "unknown"));
				default -> { /* heartbeat_ack and anything else needs no action */ }
			}
		} catch (Exception ex) {
			plugin.getLogger().warning("Ignoring malformed panel message: " + rootMessage(ex));
		}
	}

	private void sendHeartbeat() {
		if (!registered || webSocket == null) {
			return;
		}
		try {
			JsonObject heartbeat = new JsonObject();
			heartbeat.addProperty("type", "heartbeat");
			heartbeat.addProperty("server_id", config.getServerId());
			heartbeat.addProperty("timestamp", System.currentTimeMillis());
			send(heartbeat);
		} catch (Exception ex) {
			plugin.getLogger().warning("Failed to send panel heartbeat: " + rootMessage(ex));
		}
	}

	private void sendHealthReport() {
		if (!registered || webSocket == null) {
			return;
		}
		try {
			Map<String, Object> report = collector.collect();
			JsonObject message = new JsonObject();
			message.addProperty("type", "health_update");
			message.addProperty("server_id", config.getServerId());
			message.addProperty("timestamp", System.currentTimeMillis());
			message.add("payload", GSON.toJsonTree(report));
			send(message);
		} catch (Throwable ex) {
			plugin.getLogger().warning("Failed to send panel health report: " + rootMessage(ex));
		}
	}

	private void send(JsonObject message) {
		WebSocket ws = webSocket;
		if (ws == null) {
			return;
		}
		synchronized (sendLock) {
			ws.sendText(message.toString(), true);
		}
	}

	private static String rootMessage(Throwable ex) {
		Throwable current = ex;
		while ((current.getCause() != null) && (current.getCause() != current)) {
			current = current.getCause();
		}
		return current.getMessage() != null ? current.getMessage() : current.toString();
	}

	private final class Listener implements WebSocket.Listener {

		private final StringBuilder buffer = new StringBuilder();

		@Override
		public void onOpen(WebSocket webSocket) {
			webSocket.request(1);
			onSocketOpen(webSocket);
		}

		@Override
		public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
			buffer.append(data);
			if (last) {
				String text = buffer.toString();
				buffer.setLength(0);
				handleServerMessage(text);
			}
			webSocket.request(1);
			return null;
		}

		@Override
		public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
			onSocketClosed(statusCode + (reason == null || reason.isBlank() ? "" : " " + reason));
			return null;
		}

		@Override
		public void onError(WebSocket webSocket, Throwable error) {
			plugin.getLogger().warning("Panel stats link error: " + rootMessage(error));
			onSocketClosed("error");
		}
	}
}
