package org.voxelware.coretuff.dashboard.endpoints;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.dashboard.DashboardModule;
import org.voxelware.coretuff.dashboard.server.RestApiHandler;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingDefinition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ConfigEndpoints extends RestApiHandler {

    private final DashboardModule module;

    public ConfigEndpoints(DashboardModule module) {
        this.module = module;
    }

    @Override
    protected void handleInternal(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals("/api/v1/schema") && "GET".equalsIgnoreCase(method)) {
            handleSchema(exchange);
        } else if (path.equals("/api/v1/settings") && "GET".equalsIgnoreCase(method)) {
            handleGetSettings(exchange);
        } else if (path.equals("/api/v1/settings") && "PATCH".equalsIgnoreCase(method)) {
            handlePatchSettings(exchange);
        } else if (path.equals("/api/v1/reload") && "POST".equalsIgnoreCase(method)) {
            handleReload(exchange);
        } else {
            sendError(exchange, 404, "Not Found");
        }
    }

    private void handleSchema(HttpExchange exchange) throws IOException {
        var schema = module.getSettingsRegistry().getSchema();
        Map<String, Object> response = new LinkedHashMap<>();
        for (var entry : schema.entrySet()) {
            List<Map<String, Object>> defs = entry.getValue().stream()
                    .map(this::definitionToMap)
                    .collect(Collectors.toList());
            response.put(entry.getKey(), defs);
        }
        sendJson(exchange, 200, response);
    }

    private void handleGetSettings(HttpExchange exchange) throws IOException {
        var settings = module.getSettingsRegistry().getSettings();
        sendJson(exchange, 200, settings);
    }

    @SuppressWarnings("unchecked")
    private void handlePatchSettings(HttpExchange exchange) throws IOException {
        JsonObject body = parseBody(exchange);
        if (body == null || body.size() == 0) {
            sendError(exchange, 400, "Request body must contain settings to update");
            return;
        }

        var registry = module.getSettingsRegistry();
        var audit = module.getAuditService();
        String actor = "unknown";

        List<Map<String, Object>> results = new ArrayList<>();

        for (String moduleName : body.keySet()) {
            var moduleObj = body.getAsJsonObject(moduleName);
            if (moduleObj == null) continue;
            for (String key : moduleObj.keySet()) {
                var rawValue = moduleObj.get(key);
                Object value;
                if (rawValue.isJsonPrimitive()) {
                    var prim = rawValue.getAsJsonPrimitive();
                    if (prim.isNumber()) {
                        double d = prim.getAsDouble();
                        value = d == (long) d ? (long) d : d;
                    } else if (prim.isBoolean()) {
                        value = prim.getAsBoolean();
                    } else {
                        value = prim.getAsString();
                    }
                } else if (rawValue.isJsonArray()) {
                    value = rawValue.getAsJsonArray().asList();
                } else {
                    value = rawValue.toString();
                }

                var existing = registry.getSetting(moduleName, key);
                if (existing == null) {
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put("module", moduleName);
                    err.put("key", key);
                    err.put("success", false);
                    err.put("error", "Setting not found");
                    results.add(err);
                    continue;
                }

                var util = module.getSettingsUtil();
                if (!util.validateValue(value, existing.definition())) {
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put("module", moduleName);
                    err.put("key", key);
                    err.put("success", false);
                    err.put("error", "Value validation failed");
                    results.add(err);
                    continue;
                }

                Object oldValue = existing.value();
                boolean success = registry.setSetting(moduleName, key, value, actor);
                if (success) {
                    audit.log(actor, moduleName, key, oldValue, value,
                            exchange.getRemoteAddress().getHostString(), "Settings update via API");
                }

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("module", moduleName);
                result.put("key", key);
                result.put("success", success);
                results.add(result);
            }
        }

        sendJson(exchange, 200, Map.of("results", results));
    }

    @SuppressWarnings("unchecked")
    private void handleReload(HttpExchange exchange) throws IOException {
        try {
            CoreTuff plugin = module.getPlugin();
            plugin.reloadPluginState();
            module.getConfig().reload();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "ok");
            response.put("message", "All configurations reloaded successfully");
            sendJson(exchange, 200, response);
        } catch (Exception e) {
            sendError(exchange, 500, "Reload failed: " + e.getMessage());
        }
    }

    private Map<String, Object> definitionToMap(SettingDefinition def) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", def.key());
        map.put("type", def.type().getClass().getSimpleName().replace("Type", "").toLowerCase());
        map.put("default", def.defaultValue());
        map.put("description", def.description());
        map.put("category", def.category());
        if (def.min() != null) map.put("min", def.min());
        if (def.max() != null) map.put("max", def.max());
        if (def.options() != null && !def.options().isEmpty()) map.put("options", def.options());
        return map;
    }
}
