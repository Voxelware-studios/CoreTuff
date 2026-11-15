package org.voxelware.coretuff.dashboard.endpoints;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import org.voxelware.coretuff.dashboard.DashboardModule;
import org.voxelware.coretuff.dashboard.server.RestApiHandler;

import java.io.IOException;
import java.util.UUID;

public final class PairEndpoints extends RestApiHandler {

    private final DashboardModule module;

    public PairEndpoints(DashboardModule module) {
        this.module = module;
    }

    @Override
    protected void handleInternal(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals("/api/v1/pair/code") && "POST".equalsIgnoreCase(method)) {
            handleGenerateCode(exchange);
        } else if (path.equals("/api/v1/pair/exchange") && "POST".equalsIgnoreCase(method)) {
            handleExchange(exchange);
        } else if (path.startsWith("/api/v1/pair/status/") && "GET".equalsIgnoreCase(method)) {
            handleStatus(exchange);
        } else {
            sendError(exchange, 404, "Not Found");
        }
    }

    private void handleGenerateCode(HttpExchange exchange) throws IOException {
        JsonObject body = parseBody(exchange);
        String ip = body.has("ip") ? body.get("ip").getAsString() : exchange.getRemoteAddress().getHostString();
        String serverName = body.has("server_name") ? body.get("server_name").getAsString() : module.getPlugin().getServer().getName();
        String serverUuid = body.has("server_uuid") ? body.get("server_uuid").getAsString() : java.util.UUID.nameUUIDFromBytes(serverName.getBytes()).toString();

        String code = module.getPairingService().generateCode(ip, serverName, serverUuid);
        if (code == null) {
            sendError(exchange, 429, "Maximum pair sessions reached. Try again later.");
            return;
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("code", code);
        resp.addProperty("server_name", serverName);
        resp.addProperty("server_uuid", serverUuid);
        resp.addProperty("expires_in_minutes", module.getConfig().getPairCodeExpiryMinutes());
        sendJson(exchange, 201, resp);
    }

    private void handleExchange(HttpExchange exchange) throws IOException {
        JsonObject body = parseBody(exchange);
        if (!body.has("code")) {
            sendError(exchange, 400, "Missing 'code' field");
            return;
        }
        String code = body.get("code").getAsString();
        String ip = exchange.getRemoteAddress().getHostString();

        var result = module.getPairingService().exchangeCode(code, ip);
        if (result == null) {
            sendError(exchange, 401, "Invalid or expired pair code");
            return;
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("token", result.token());
        resp.addProperty("token_type", "Bearer");
        resp.addProperty("server_name", result.serverName());
        resp.addProperty("server_uuid", result.serverUuid());
        resp.addProperty("expires_in_hours", result.expiresInHours());
        sendJson(exchange, 200, resp);
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String code = path.substring("/api/v1/pair/status/".length());
        if (code.isBlank()) {
            sendError(exchange, 400, "Missing code parameter");
            return;
        }

        boolean valid = module.getPairingService().isValidCode(code);

        JsonObject resp = new JsonObject();
        resp.addProperty("code", code);
        resp.addProperty("valid", valid);
        sendJson(exchange, 200, resp);
    }
}
