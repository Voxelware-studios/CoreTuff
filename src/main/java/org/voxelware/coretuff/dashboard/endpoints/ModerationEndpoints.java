package org.voxelware.coretuff.dashboard.endpoints;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import org.voxelware.coretuff.Features.Moderation.PunishmentEngine.ModerationSummary;
import org.voxelware.coretuff.Features.Moderation.PunishmentEngine.ModeratorStat;
import org.voxelware.coretuff.Features.Moderation.PunishmentEngine.PagedResult;
import org.voxelware.coretuff.Features.Moderation.PunishmentEngine.PunishmentRecord;
import org.voxelware.coretuff.Features.Moderation.Service.ModerationService;
import org.voxelware.coretuff.dashboard.DashboardModule;
import org.voxelware.coretuff.dashboard.server.RestApiHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModerationEndpoints extends RestApiHandler {

    private final DashboardModule module;

    public ModerationEndpoints(DashboardModule module) {
        this.module = module;
    }

    @Override
    protected void handleInternal(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (!"GET".equalsIgnoreCase(method)) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        ModerationService svc = module.getPlugin().moderationService();
        if (svc == null) {
            sendError(exchange, 503, "Moderation service not available");
            return;
        }

        if (path.equals("/api/v1/moderation/summary")) {
            handleSummary(exchange, svc);
        } else if (path.equals("/api/v1/moderation/punishments")) {
            handlePunishments(exchange, svc);
        } else if (path.startsWith("/api/v1/moderation/players/")) {
            handlePlayerHistory(exchange, svc, path);
        } else if (path.equals("/api/v1/moderation/moderators")) {
            handleModerators(exchange, svc);
        } else if (path.startsWith("/api/v1/moderation/punishments/")) {
            handlePunishmentDetail(exchange, svc, path);
        } else {
            sendError(exchange, 404, "Not found");
        }
    }

    private void handleSummary(HttpExchange exchange, ModerationService svc) throws IOException {
        try {
            ModerationSummary summary = svc.getSummary();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("totalPunishments", summary.getTotalPunishments());
            data.put("activePunishments", summary.getActivePunishments());
            data.put("bans", summary.getBans());
            data.put("tempBans", summary.getTempBans());
            data.put("ipBans", summary.getIpBans());
            data.put("pardoned", summary.getPardoned());
            data.put("expired", summary.getExpired());
            data.put("thisWeek", summary.getThisWeek());
            data.put("thisMonth", summary.getThisMonth());
            sendJson(exchange, 200, data);
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to load summary: " + e.getMessage());
        }
    }

    private void handlePunishments(HttpExchange exchange, ModerationService svc) throws IOException {
        try {
            Map<String, String> params = parseQueryParams(exchange);
            int page = parseIntParam(params, "page", 1);
            int pageSize = parseIntParam(params, "pageSize", 25);
            String search = params.get("search");
            String type = params.get("type");
            String status = params.get("status");
            String sort = params.get("sort");
            String order = params.get("order");

            PagedResult<PunishmentRecord> result = svc.getPunishmentsPaginated(
                    page, pageSize, search, type, status, sort, order);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", toJsonArray(result.getItems()));
            data.put("total", result.getTotal());
            data.put("page", result.getPage());
            data.put("pageSize", result.getPageSize());
            data.put("totalPages", result.getTotalPages());
            sendJson(exchange, 200, data);
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to load punishments: " + e.getMessage());
        }
    }

    private void handlePunishmentDetail(HttpExchange exchange, ModerationService svc, String path) throws IOException {
        try {
            String id = path.substring("/api/v1/moderation/punishments/".length());
            if (id.isEmpty()) {
                sendError(exchange, 400, "Missing punishment ID");
                return;
            }
            PunishmentRecord record = svc.getPunishmentById(id);
            if (record == null) {
                sendError(exchange, 404, "Punishment not found");
                return;
            }
            sendJson(exchange, 200, recordToMap(record));
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to load punishment: " + e.getMessage());
        }
    }

    private void handlePlayerHistory(HttpExchange exchange, ModerationService svc, String path) throws IOException {
        try {
            String uuid = path.substring("/api/v1/moderation/players/".length());
            if (uuid.endsWith("/history")) {
                uuid = uuid.substring(0, uuid.length() - "/history".length());
            }
            if (uuid.isEmpty()) {
                sendError(exchange, 400, "Missing player UUID");
                return;
            }
            Map<String, String> params = parseQueryParams(exchange);
            int page = parseIntParam(params, "page", 1);
            int pageSize = parseIntParam(params, "pageSize", 25);

            PagedResult<PunishmentRecord> result = svc.getPlayerHistory(uuid, page, pageSize);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("items", toJsonArray(result.getItems()));
            data.put("total", result.getTotal());
            data.put("page", result.getPage());
            data.put("pageSize", result.getPageSize());
            data.put("totalPages", result.getTotalPages());
            sendJson(exchange, 200, data);
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to load player history: " + e.getMessage());
        }
    }

    private void handleModerators(HttpExchange exchange, ModerationService svc) throws IOException {
        try {
            List<ModeratorStat> stats = svc.getModeratorStats();
            JsonArray arr = new JsonArray();
            for (ModeratorStat stat : stats) {
                JsonObject obj = new JsonObject();
                obj.addProperty("moderator", stat.getModerator());
                obj.addProperty("count", stat.getCount());
                arr.add(obj);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("moderators", new com.google.gson.Gson().fromJson(arr, Object.class));
            sendJson(exchange, 200, data);
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to load moderator stats: " + e.getMessage());
        }
    }

    private int parseIntParam(Map<String, String> params, String key, int defaultVal) {
        String val = params.get(key);
        if (val == null) return defaultVal;
        try {
            int parsed = Integer.parseInt(val);
            return parsed > 0 ? parsed : defaultVal;
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private JsonArray toJsonArray(List<PunishmentRecord> records) {
        JsonArray arr = new JsonArray();
        for (PunishmentRecord r : records) {
            arr.add(new com.google.gson.Gson().toJsonTree(recordToMap(r)));
        }
        return arr;
    }

    private Map<String, Object> recordToMap(PunishmentRecord r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        Map<String, Object> player = new LinkedHashMap<>();
        player.put("uuid", r.getUuid());
        player.put("name", r.getName());
        map.put("player", player);
        map.put("type", r.getType());
        map.put("status", deriveStatus(r));
        map.put("reason", r.getReason());
        map.put("moderator", r.getModerator());
        map.put("issuedAt", r.getCreatedAt());
        map.put("expiresAt", r.getExpiresAt());
        if (r.getIp() != null) map.put("ip", r.getIp());
        if (r.isPermanent()) {
            map.put("duration", "Permanent");
        } else if (r.getExpiresAt() != -1) {
            long durationMs = r.getExpiresAt() - r.getCreatedAt();
            map.put("duration", formatDuration(durationMs));
        }
        if (r.isExpired() && !r.isActive()) {
            map.put("expiredAt", r.getExpiresAt());
        }
        if (!r.isActive()) {
            map.put("pardonedBy", r.getPardonedBy());
            map.put("pardonReason", r.getPardonReason());
            map.put("removedAt", r.getRemovedAt());
        }
        return map;
    }

    private String deriveStatus(PunishmentRecord r) {
        if (!r.isActive()) return "PARDONED";
        if (r.isExpired()) return "EXPIRED";
        return "ACTIVE";
    }

    private String formatDuration(long millis) {
        if (millis <= 0) return "0s";
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 && days == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
