package org.voxelware.coretuff.dashboard.websocket;

import com.sun.net.httpserver.HttpHandler;

import org.voxelware.coretuff.dashboard.DashboardConfig;
import org.voxelware.coretuff.dashboard.auth.JwtProvider;
import org.voxelware.coretuff.dashboard.health.HealthReport;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class WebSocketManager {

    private static final Logger LOG = Logger.getLogger(WebSocketManager.class.getName());

    private final WebSocketHandler handler;
    private final DashboardConfig config;
    private final SettingsRegistry settingsRegistry;
    private ScheduledExecutorService heartbeatExecutor;

    public WebSocketManager(JwtProvider jwtProvider, DashboardConfig config, SettingsRegistry settingsRegistry) {
        this.config = config;
        this.settingsRegistry = settingsRegistry;
        this.handler = new WebSocketHandler(jwtProvider, settingsRegistry);
    }

    public HttpHandler getHttpHandler() {
        return handler;
    }

    public void startHeartbeat() {
        if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) return;
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ct-ws-heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                handler.broadcast("{\"type\":\"ping\"}");
            } catch (Exception ignored) {}
        }, 30, 30, TimeUnit.SECONDS);
    }

    public void stop() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
        }
        for (var client : handler.getClients()) {
            try {
                client.close();
            } catch (Exception ignored) {}
        }
    }

    public void broadcastSettingUpdate(String module, String key, Object oldValue, Object newValue, String actor) {
        String message = """
                {
                  "type": "setting_updated",
                  "payload": {
                    "module": "%s",
                    "key": "%s",
                    "oldValue": %s,
                    "newValue": %s,
                    "actor": "%s"
                  },
                  "timestamp": %d
                }""".formatted(escapeJson(module), escapeJson(key),
                oldValue != null ? "\"" + escapeJson(String.valueOf(oldValue)) + "\"" : "null",
                newValue != null ? "\"" + escapeJson(String.valueOf(newValue)) + "\"" : "null",
                escapeJson(actor), System.currentTimeMillis());
        handler.broadcast(message);
    }

    public void broadcastReload() {
        handler.broadcast("""  
                {"type":"reload","timestamp":%d}
                """.formatted(System.currentTimeMillis()));
    }

    public void broadcastMetricsUpdate(Object metricsData) {
        String json = new com.google.gson.Gson().toJson(metricsData);
        String message = """
                {
                  "type": "metrics_update",
                  "payload": %s,
                  "timestamp": %d
                }""".formatted(json, System.currentTimeMillis());
        handler.broadcast(message);
    }

    public void broadcastConsoleLog(String logEntry) {
        String message = """
                {
                  "type": "console_log",
                  "payload": {
                    "message": "%s"
                  },
                  "timestamp": %d
                }""".formatted(escapeJson(logEntry), System.currentTimeMillis());
        handler.broadcast(message);
    }

    public void broadcastHealthUpdate(HealthReport report) {
        String json = new com.google.gson.Gson().toJson(report.toMap());
        String message = """
                {
                  "type": "health_update",
                  "payload": %s,
                  "timestamp": %d
                }""".formatted(json, System.currentTimeMillis());
        handler.broadcast(message);
    }

    public int getConnectedClients() {
        return handler.getClientCount();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
