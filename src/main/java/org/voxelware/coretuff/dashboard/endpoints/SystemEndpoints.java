package org.voxelware.coretuff.dashboard.endpoints;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.dashboard.DashboardModule;
import org.voxelware.coretuff.dashboard.server.RestApiHandler;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SystemEndpoints extends RestApiHandler {

    private final DashboardModule module;

    public SystemEndpoints(DashboardModule module) {
        this.module = module;
    }

    @Override
    protected void handleInternal(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (!"GET".equalsIgnoreCase(method)) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        switch (path) {
            case "/api/v1/modules" -> handleModules(exchange);
            case "/api/v1/version" -> handleVersion(exchange);
            case "/api/v1/metrics" -> handleMetrics(exchange);
            case "/api/v1/server" -> handleServer(exchange);
            case "/api/v1/health" -> handleHealth(exchange);
            case "/api/v1/health/full" -> handleHealthFull(exchange);
            case "/api/v1/health/plugins" -> handleHealthPlugins(exchange);
            case "/api/v1/health/integrations" -> handleHealthIntegrations(exchange);
            default -> sendError(exchange, 404, "Not Found");
        }
    }

    private void handleModules(HttpExchange exchange) throws IOException {
        Map<String, Object> modules = new LinkedHashMap<>();
        modules.put("dashboard", module.isRunning());
        modules.put("economy", module.getPlugin().economyService() != null);
        modules.put("moderation", module.getPlugin().moderationService() != null);
        modules.put("warps", module.getPlugin().warpService() != null);
        modules.put("jails", module.getPlugin().jailService() != null);
        modules.put("kits", module.getPlugin().kitService() != null);
        sendJson(exchange, 200, modules);
    }

    private void handleVersion(HttpExchange exchange) throws IOException {
        CoreTuff plugin = module.getPlugin();
        Map<String, Object> version = new LinkedHashMap<>();
        version.put("name", plugin.getName());
        version.put("version", plugin.getDescription().getVersion());
        version.put("channel", plugin.getChannel());
        version.put("apiVersion", plugin.getDescription().getAPIVersion());
        version.put("serverVersion", Bukkit.getVersion());
        version.put("serverName", Bukkit.getName());
        version.put("folia", plugin.isFolia());
        sendJson(exchange, 200, version);
    }

    private void handleMetrics(HttpExchange exchange) throws IOException {
        var metrics = module.getMetricsCollector().getCurrent();
        sendJson(exchange, 200, metrics);
    }

    private void handleServer(HttpExchange exchange) throws IOException {
        Server server = Bukkit.getServer();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("tps", server.getTPS());
        info.put("maxPlayers", server.getMaxPlayers());
        info.put("onlinePlayers", server.getOnlinePlayers().size());
        info.put("motd", server.getMotd());
        info.put("bukkitVersion", server.getBukkitVersion());
        info.put("minecraftVersion", server.getMinecraftVersion());
        info.put("worlds", server.getWorlds().size());
        info.put("port", server.getPort());
        info.put("hasWhitelist", server.hasWhitelist());
        sendJson(exchange, 200, info);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", module.isRunning() ? "healthy" : "degraded");
        health.put("dashboard", module.isRunning());
        health.put("database", module.getPlugin().databaseManager() != null
                && module.getPlugin().databaseManager().getConnection() != null);
        health.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
        sendJson(exchange, 200, health);
    }

    private void handleHealthFull(HttpExchange exchange) throws IOException {
        var monitor = module.getHealthMonitor();
        if (monitor == null) {
            sendError(exchange, 503, "Health monitor not available");
            return;
        }
        var report = monitor.getLastReport();
        sendJson(exchange, 200, report.toMap());
    }

    private void handleHealthPlugins(HttpExchange exchange) throws IOException {
        var monitor = module.getHealthMonitor();
        if (monitor == null) {
            sendError(exchange, 503, "Health monitor not available");
            return;
        }
        var report = monitor.getLastReport();
        for (var component : report.components()) {
            if ("Plugins".equals(component.name())) {
                sendJson(exchange, 200, component.details());
                return;
            }
        }
        sendError(exchange, 404, "Plugin health data not found");
    }

    private void handleHealthIntegrations(HttpExchange exchange) throws IOException {
        var monitor = module.getHealthMonitor();
        if (monitor == null) {
            sendError(exchange, 503, "Health monitor not available");
            return;
        }
        var report = monitor.getLastReport();
        for (var component : report.components()) {
            if ("Integrations".equals(component.name())) {
                sendJson(exchange, 200, component.details());
                return;
            }
        }
        sendError(exchange, 404, "Integration health data not found");
    }
}
