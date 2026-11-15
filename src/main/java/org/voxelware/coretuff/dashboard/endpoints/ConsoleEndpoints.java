package org.voxelware.coretuff.dashboard.endpoints;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import org.bukkit.Bukkit;
import org.voxelware.coretuff.dashboard.DashboardModule;
import org.voxelware.coretuff.dashboard.server.RestApiHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConsoleEndpoints extends RestApiHandler {

    private final DashboardModule module;

    public ConsoleEndpoints(DashboardModule module) {
        this.module = module;
    }

    @Override
    protected void handleInternal(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals("/api/v1/console/logs") && "GET".equalsIgnoreCase(method)) {
            handleGetLogs(exchange);
        } else if (path.equals("/api/v1/console/execute") && "POST".equalsIgnoreCase(method)) {
            handleExecute(exchange);
        } else {
            sendError(exchange, 404, "Not Found");
        }
    }

    private void handleGetLogs(HttpExchange exchange) throws IOException {
        var params = parseQueryParams(exchange);
        int count = 100;
        if (params.containsKey("count")) {
            try {
                count = Math.max(1, Math.min(5000, Integer.parseInt(params.get("count"))));
            } catch (NumberFormatException ignored) {}
        }
        String search = params.get("search");

        var collector = module.getConsoleLogCollector();
        var entries = search != null ? collector.search(search) : collector.getRecent(count);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count", entries.size());
        response.put("entries", entries);
        sendJson(exchange, 200, response);
    }

    private void handleExecute(HttpExchange exchange) throws IOException {
        JsonObject body = parseBody(exchange);
        if (body == null || !body.has("command")) {
            sendError(exchange, 400, "Missing 'command' field");
            return;
        }
        String command = body.get("command").getAsString();
        if (command.isBlank()) {
            sendError(exchange, 400, "Command cannot be empty");
            return;
        }

        try {
            boolean success = Bukkit.getScheduler().callSyncMethod(module.getPlugin(), () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            ).get();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", success);
            response.put("command", command);
            response.put("executed_by", "dashboard-api");
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to execute command: " + e.getMessage());
        }
    }
}
