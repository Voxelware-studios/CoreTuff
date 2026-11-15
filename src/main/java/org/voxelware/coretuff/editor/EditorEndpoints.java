package org.voxelware.coretuff.editor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EditorEndpoints {

    private final EditorModule module;
    private static final Gson GSON = new Gson();

    public EditorEndpoints(EditorModule module) {
        this.module = module;
    }

    public JsonObject handleApply(JsonObject body) {
        JsonArray patch = body.has("patch") ? body.getAsJsonArray("patch") : null;
        if (patch == null || patch.isEmpty()) {
            return errorResponse(400, "Missing 'patch' field");
        }

        var registry = module.getSettingsRegistry();
        String actor = "web-editor";
        List<Map<String, Object>> results = new ArrayList<>();

        for (int i = 0; i < patch.size(); i++) {
            JsonObject op = patch.get(i).getAsJsonObject();
            if (!op.has("op") || !op.has("path")) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("index", i);
                err.put("success", false);
                err.put("error", "Missing 'op' or 'path'");
                results.add(err);
                continue;
            }

            String opType = op.get("op").getAsString();
            String pathStr = op.get("path").getAsString();

            if (!"replace".equals(opType)) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("index", i);
                err.put("success", false);
                err.put("error", "Unsupported operation: " + opType);
                results.add(err);
                continue;
            }

            if (pathStr.startsWith("/")) pathStr = pathStr.substring(1);
            String[] parts = pathStr.split("/", 2);
            if (parts.length < 2) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("index", i);
                err.put("success", false);
                err.put("error", "Invalid path: " + op.get("path").getAsString());
                results.add(err);
                continue;
            }

            String moduleName = parts[0];
            String key = parts[1];
            Object value = jsonElementToValue(op.get("value"));

            var existing = registry.getSetting(moduleName, key);
            if (existing == null) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("index", i);
                err.put("success", false);
                err.put("error", "Setting '" + moduleName + "/" + key + "' not found");
                results.add(err);
                continue;
            }

            Object oldValue = existing.value();
            boolean success = registry.setSetting(moduleName, key, value, actor);
            if (success) {
                var configService = module.getPlugin().getConfigurationService();
                if (configService != null) {
                    configService.set(moduleName, key, valueToString(value));
                }
                if (!java.util.Objects.equals(oldValue, value)) {
                    module.getPlugin().getLogger().info("Web Editor changed " + moduleName + "/" + key + ": " + oldValue + " -> " + value);
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("index", i);
            result.put("module", moduleName);
            result.put("key", key);
            result.put("success", success);
            results.add(result);
        }

        boolean allSuccess = results.stream().allMatch(r -> (boolean) r.get("success"));

        if (allSuccess) {
            try {
                module.getPlugin().reloadPluginState();
            } catch (Exception ignored) {}
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", results);
        response.put("status", allSuccess ? "applied" : "partial");

        JsonObject json = GSON.toJsonTree(response).getAsJsonObject();
        return json;
    }

    public JsonObject handleCancel() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "cancelled");
        response.put("message", "Session cancelled.");
        return GSON.toJsonTree(response).getAsJsonObject();
    }

    private JsonObject errorResponse(int status, String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("error", message);
        err.put("status", status);
        return GSON.toJsonTree(err).getAsJsonObject();
    }

    private String valueToString(Object value) {
        if (value == null) return "";
        if (value instanceof List || value instanceof Map) {
            return GSON.toJson(value);
        }
        return value.toString();
    }

    private Object jsonElementToValue(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonPrimitive()) {
            var prim = el.getAsJsonPrimitive();
            if (prim.isNumber()) {
                double d = prim.getAsDouble();
                return d == (long) d ? (long) d : d;
            } else if (prim.isBoolean()) {
                return prim.getAsBoolean();
            }
            return prim.getAsString();
        } else if (el.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (var e : el.getAsJsonArray()) {
                list.add(jsonElementToValue(e));
            }
            return list;
        } else if (el.isJsonObject()) {
            return GSON.fromJson(el, Map.class);
        }
        return el.toString();
    }
}
