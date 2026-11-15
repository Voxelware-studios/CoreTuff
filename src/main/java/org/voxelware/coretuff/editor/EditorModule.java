package org.voxelware.coretuff.editor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.Features.Moderation.PunishmentEngine.ModerationSummary;
import org.voxelware.coretuff.Features.Moderation.PunishmentEngine.ModeratorStat;
import org.voxelware.coretuff.Features.Moderation.PunishmentEngine.PagedResult;
import org.voxelware.coretuff.Features.Moderation.PunishmentEngine.PunishmentRecord;
import org.voxelware.coretuff.Features.Moderation.Service.ModerationService;
import org.voxelware.coretuff.dashboard.health.HealthMonitor;
import org.voxelware.coretuff.dashboard.health.HealthReport;
import org.voxelware.coretuff.dashboard.health.IncidentSeverity;
import org.voxelware.coretuff.dashboard.health.IncidentType;
import org.voxelware.coretuff.dashboard.players.PlayerData;
import org.voxelware.coretuff.dashboard.players.PlayerDataService;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class EditorModule {

    private static final Logger LOG = Logger.getLogger(EditorModule.class.getName());
    private static final long HEARTBEAT_INTERVAL_SECONDS = 10;

    private final CoreTuff plugin;
    private final EditorConfig config;
    private final EditorEndpoints endpoints;
    private HttpClient httpClient;
    private WebSocket webSocket;
    private boolean running;
    private ScheduledExecutorService reconnectExecutor;
    private ScheduledExecutorService heartbeatExecutor;
    private int reconnectDelay = 2;

    private final Set<String> activeSessions = ConcurrentHashMap.newKeySet();
    private volatile boolean heartbeatActive = false;
    private HealthMonitor healthMonitor;

    public EditorModule(CoreTuff plugin) {
        this.plugin = plugin;
        this.config = new EditorConfig();
        this.config.setDevMode(plugin.isDevMode());
        this.endpoints = new EditorEndpoints(this);
        var cs = plugin.getConfigurationService();
        if (cs != null) config.setConfigurationService(cs);
    }

    public EditorConfig getConfig() {
        return config;
    }

    public synchronized void start() {
        if (running) {
            LOG.info("Editor module already running.");
            return;
        }
        if (!config.isEnabled()) {
            LOG.info("Editor module is disabled in config.");
            return;
        }

        this.httpClient = HttpClient.newHttpClient();
        this.reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
        connect();
    }

    private void connect() {
        String backendUrl = config.getBackendUrl();
        String wsUrl = backendUrl
                .replace("http://", "ws://")
                .replace("https://", "wss://")
                + "/ws";

        LOG.info("Connecting to editor backend WebSocket: " + wsUrl);

        httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new WebSocketListener())
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    this.running = true;
                    this.reconnectDelay = 2;
                    LOG.info("Connected to editor backend");

                    JsonObject register = new JsonObject();
                    register.addProperty("type", "register");
                    register.addProperty("server_id", config.getServerId());
                    register.addProperty("server_name", plugin.getServer().getName());
                    register.addProperty("plugin_version", plugin.getPluginMeta().getVersion());
                    ws.sendText(register.toString(), true);

                    if (!activeSessions.isEmpty()) {
                        startHeartbeat();
                    }

                    startHealthMonitor();
                })
                .exceptionally(ex -> {
                    LOG.warning("Failed to connect to editor backend: " + ex.getMessage());
                    scheduleReconnect();
                    return null;
                });
    }

    private void scheduleReconnect() {
        if (reconnectExecutor == null || reconnectExecutor.isShutdown()) return;
        LOG.info("Reconnecting in " + reconnectDelay + "s...");
        reconnectExecutor.schedule(this::connect, reconnectDelay, TimeUnit.SECONDS);
        reconnectDelay = Math.min(reconnectDelay * 2, 60);
    }

    public synchronized void stop() {
        if (!running && webSocket == null) return;
        running = false;
        stopHeartbeat();
        stopHealthMonitor();

        if (reconnectExecutor != null) {
            reconnectExecutor.shutdownNow();
            reconnectExecutor = null;
        }

        if (webSocket != null) {
            webSocket.sendClose(1000, "shutdown");
            webSocket = null;
        }

        activeSessions.clear();
        LOG.info("Editor module stopped.");
    }

    public boolean isRunning() {
        return running;
    }

    public CoreTuff getPlugin() {
        return plugin;
    }

    public SettingsRegistry getSettingsRegistry() {
        return plugin.getSettingsRegistry();
    }

    public void sendMessage(JsonObject message) {
        if (webSocket != null && running) {
            webSocket.sendText(message.toString(), true);
        }
    }

    public void activateSession(String sessionId) {
        activeSessions.add(sessionId);
        if (running && !heartbeatActive) {
            startHeartbeat();
        }
        LOG.info("Dashboard session activated: " + sessionId + " (total: " + activeSessions.size() + ")");
    }

    public void deactivateSession(String sessionId) {
        activeSessions.remove(sessionId);
        if (activeSessions.isEmpty() && heartbeatActive) {
            stopHeartbeat();
        }
        LOG.info("Dashboard session deactivated: " + sessionId + " (total: " + activeSessions.size() + ")");
    }

    public Set<String> getActiveSessions() {
        return Set.copyOf(activeSessions);
    }

    private void startHeartbeat() {
        if (heartbeatActive) return;
        heartbeatActive = true;
        if (heartbeatExecutor == null || heartbeatExecutor.isShutdown()) {
            heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ct-heartbeat");
                t.setDaemon(true);
                return t;
            });
        }
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat,
                HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        LOG.info("Heartbeat started (interval: " + HEARTBEAT_INTERVAL_SECONDS + "s)");
    }

    private void stopHeartbeat() {
        heartbeatActive = false;
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
            heartbeatExecutor = null;
        }
        LOG.info("Heartbeat stopped.");
    }

    private void sendHeartbeat() {
        if (!running || webSocket == null || activeSessions.isEmpty()) return;
        try {
            JsonObject heartbeat = new JsonObject();
            heartbeat.addProperty("type", "heartbeat");
            heartbeat.addProperty("server_id", config.getServerId());
            heartbeat.addProperty("timestamp", System.currentTimeMillis());
            sendMessage(heartbeat);
        } catch (Exception e) {
            LOG.warning("Failed to send heartbeat: " + e.getMessage());
        }
    }

    public void broadcastPlayerSnapshot() {
        if (!running || webSocket == null) return;
        PlayerDataService svc = plugin.playerDataService();
        if (svc == null) return;
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("type", "player_snapshot");
            payload.add("payload", new Gson().toJsonTree(svc.getPlayerSnapshot()));
            payload.addProperty("timestamp", System.currentTimeMillis());
            sendMessage(payload);
        } catch (Exception e) {
            LOG.warning("Failed to broadcast player snapshot: " + e.getMessage());
        }
    }

    private void startHealthMonitor() {
        if (healthMonitor != null) return;
        healthMonitor = new HealthMonitor(plugin);
        healthMonitor.start();
        healthMonitor.addListener(this::forwardHealthToBackend);
        LOG.info("Health monitor started in EditorModule");
    }

    private void stopHealthMonitor() {
        if (healthMonitor != null) {
            healthMonitor.stop();
            healthMonitor = null;
        }
    }

    public HealthMonitor getHealthMonitor() {
        return healthMonitor;
    }

    private void forwardHealthToBackend(HealthReport report) {
        if (!running || webSocket == null) return;
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("type", "health_update");
            payload.add("payload", new Gson().toJsonTree(report.toMap()));
            sendMessage(payload);
        } catch (Exception e) {
            LOG.warning("Failed to forward health to backend: " + e.getMessage());
        }
    }

    public void broadcastModerationUpdate() {
        if (!running || webSocket == null) return;
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("type", "moderation_update");
            payload.addProperty("timestamp", System.currentTimeMillis());
            sendMessage(payload);
        } catch (Exception e) {
            LOG.warning("Failed to broadcast moderation update: " + e.getMessage());
        }
    }

    private void handleModerationQuery(JsonObject msg) {
        String requestId = msg.has("requestId") ? msg.get("requestId").getAsString() : null;
        String action = msg.has("action") ? msg.get("action").getAsString() : null;
        if (requestId == null || action == null) return;

        ModerationService svc = plugin.moderationService();
        if (svc == null) {
            sendModerationResponse(requestId, null, "Moderation service not available");
            return;
        }

        try {
            JsonObject params = msg.has("params") ? msg.getAsJsonObject("params") : new JsonObject();
            Object result = switch (action) {
                case "moderation_summary" -> svc.getSummary();
                case "moderation_list" -> {
                    int page = params.has("page") ? params.get("page").getAsInt() : 1;
                    int pageSize = params.has("pageSize") ? params.get("pageSize").getAsInt() : 25;
                    String search = params.has("search") && !params.get("search").isJsonNull() ? params.get("search").getAsString() : null;
                    String type = params.has("type") && !params.get("type").isJsonNull() ? params.get("type").getAsString() : null;
                    String status = params.has("status") && !params.get("status").isJsonNull() ? params.get("status").getAsString() : null;
                    String sort = params.has("sort") && !params.get("sort").isJsonNull() ? params.get("sort").getAsString() : null;
                    String order = params.has("order") && !params.get("order").isJsonNull() ? params.get("order").getAsString() : null;
                    yield svc.getPunishmentsPaginated(page, pageSize, search, type, status, sort, order);
                }
                case "moderation_detail" -> {
                    String id = params.has("id") ? params.get("id").getAsString() : null;
                    yield id != null ? svc.getPunishmentById(id) : null;
                }
                case "moderation_player_history" -> {
                    String uuid = params.has("uuid") ? params.get("uuid").getAsString() : null;
                    int page = params.has("page") ? params.get("page").getAsInt() : 1;
                    int pageSize = params.has("pageSize") ? params.get("pageSize").getAsInt() : 25;
                    yield uuid != null ? svc.getPlayerHistory(uuid, page, pageSize) : null;
                }
                case "moderation_moderators" -> svc.getModeratorStats();
                default -> null;
            };

            if (result == null) {
                sendModerationResponse(requestId, null, "No data");
                return;
            }

            JsonObject response = new JsonObject();
            response.addProperty("type", "moderation_response");
            response.addProperty("requestId", requestId);
            response.add("result", new Gson().toJsonTree(result, result.getClass()));
            sendMessage(response);
        } catch (Exception e) {
            LOG.warning("Error handling moderation query: " + e.getMessage());
            sendModerationResponse(requestId, null, "Error: " + e.getMessage());
        }
    }

    private void sendModerationResponse(String requestId, Object result, String error) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "moderation_response");
        response.addProperty("requestId", requestId);
        if (error != null) response.addProperty("error", error);
        if (result != null) response.add("result", new Gson().toJsonTree(result, result.getClass()));
        sendMessage(response);
    }

    private void handlePlayerQuery(JsonObject msg) {
        String requestId = msg.has("requestId") ? msg.get("requestId").getAsString() : null;
        String action = msg.has("action") ? msg.get("action").getAsString() : null;
        if (requestId == null || action == null) return;

        PlayerDataService svc = plugin.playerDataService();
        if (svc == null) {
            sendPlayerResponse(requestId, null, "Player service not available");
            return;
        }

        try {
            JsonObject params = msg.has("params") ? msg.getAsJsonObject("params") : new JsonObject();
            Object result = switch (action) {
                case "player_list" -> svc.getPlayerSnapshot();
                case "player_detail" -> {
                    String uuid = params.has("uuid") ? params.get("uuid").getAsString() : null;
                    yield uuid != null ? svc.getPlayer(java.util.UUID.fromString(uuid)) : null;
                }
                case "player_search" -> {
                    String query = params.has("query") ? params.get("query").getAsString() : null;
                    yield query != null ? svc.searchPlayers(query) : java.util.List.of();
                }
                default -> null;
            };

            if (result == null) {
                sendPlayerResponse(requestId, null, "No data");
                return;
            }

            JsonObject response = new JsonObject();
            response.addProperty("type", "player_response");
            response.addProperty("requestId", requestId);
            response.add("result", new Gson().toJsonTree(result, result.getClass()));
            sendMessage(response);
        } catch (Exception e) {
            LOG.warning("Error handling player query: " + e.getMessage());
            sendPlayerResponse(requestId, null, "Error: " + e.getMessage());
        }
    }

    private void sendPlayerResponse(String requestId, Object result, String error) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "player_response");
        response.addProperty("requestId", requestId);
        if (error != null) response.addProperty("error", error);
        if (result != null) response.add("result", new Gson().toJsonTree(result, result.getClass()));
        sendMessage(response);
    }

    private class WebSocketListener implements WebSocket.Listener {

        private final StringBuilder messageBuffer = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            messageBuffer.append(data);
            if (last) {
                String raw = messageBuffer.toString();
                messageBuffer.setLength(0);
                try {
                    handleMessage(raw);
                } catch (Exception e) {
                    LOG.warning("Error handling WebSocket message: " + e.getMessage());
                }
            }
            return WebSocket.Listener.super.onText(ws, data, last);
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            LOG.warning("WebSocket error: " + error.getMessage());
            logSessionExpired("Backend connection failure");
            ws.abort();
            running = false;
            webSocket = null;
            scheduleReconnect();
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            LOG.info("WebSocket closed: " + statusCode + " " + reason);
            if (statusCode == 1000) {
                logSessionClosed("Normal closure");
            } else if (statusCode == 1001) {
                logSessionExpired("Server shutting down");
            } else {
                logSessionExpired("Connection lost (code: " + statusCode + ")");
            }
            running = false;
            webSocket = null;
            if (statusCode != 1000) {
                scheduleReconnect();
            }
            return WebSocket.Listener.super.onClose(ws, statusCode, reason);
        }

        private void handleMessage(String raw) {
            JsonObject msg = JsonParser.parseString(raw).getAsJsonObject();
            String type = msg.has("type") ? msg.get("type").getAsString() : null;

            if ("apply".equals(type)) {
                JsonObject result = endpoints.handleApply(msg);
                result.addProperty("type", "apply_result");
                result.addProperty("session_id",
                        msg.has("session_id") ? msg.get("session_id").getAsString() : "");
                sendMessage(result);
            } else if ("cancel".equals(type)) {
                JsonObject result = endpoints.handleCancel();
                result.addProperty("type", "cancel_ack");
                result.addProperty("session_id",
                        msg.has("session_id") ? msg.get("session_id").getAsString() : "");
                sendMessage(result);
            } else if ("heartbeat_ack".equals(type)) {
                // Heartbeat acknowledged by backend - server is alive
            } else if ("session_activated".equals(type)) {
                String sessionId = msg.has("session_id") ? msg.get("session_id").getAsString() : "";
                if (!sessionId.isEmpty()) {
                    activateSession(sessionId);
                }
            } else if ("session_deactivated".equals(type)) {
                String sessionId = msg.has("session_id") ? msg.get("session_id").getAsString() : "";
                if (!sessionId.isEmpty()) {
                    deactivateSession(sessionId);
                }
            } else if ("session_expired".equals(type)) {
                String reason = msg.has("reason") ? msg.get("reason").getAsString() : "Unknown";
                logSessionExpired(reason);
            } else if ("moderation_query".equals(type)) {
                handleModerationQuery(msg);
            } else if ("player_query".equals(type)) {
                handlePlayerQuery(msg);
            }
        }
    }

    private void logSessionExpired(String reason) {
        if (!activeSessions.isEmpty()) {
            plugin.getLogger().info("[CoreTuff] Dashboard session expired.");
            plugin.getLogger().info("[CoreTuff] Reason: " + reason);
            if (healthMonitor != null) {
                healthMonitor.getIncidentManager().createIncident(
                    IncidentType.WEB_EDITOR,
                    "Web Editor session expired",
                    IncidentSeverity.WARNING,
                    "Reason: " + reason
                );
            }
        }
    }

    private void logSessionClosed(String reason) {
        plugin.getLogger().info("[CoreTuff] Dashboard session closed.");
        plugin.getLogger().info("[CoreTuff] Reason: " + reason);
    }
}
