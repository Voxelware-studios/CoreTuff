package org.voxelware.coretuff.dashboard.endpoints;

import com.sun.net.httpserver.HttpExchange;
import org.voxelware.coretuff.dashboard.DashboardModule;
import org.voxelware.coretuff.dashboard.players.PlayerData;
import org.voxelware.coretuff.dashboard.players.PlayerDataService;
import org.voxelware.coretuff.dashboard.server.RestApiHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlayerEndpoints extends RestApiHandler {

    private final DashboardModule module;

    public PlayerEndpoints(DashboardModule module) {
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

        PlayerDataService svc = module.getPlayerDataService();
        if (svc == null) {
            sendError(exchange, 503, "Player service not available");
            return;
        }

        if (path.equals("/api/v1/players")) {
            handlePlayerList(exchange, svc);
        } else if (path.equals("/api/v1/players/search") || path.startsWith("/api/v1/players/search?")) {
            handlePlayerSearch(exchange, svc, path);
        } else if (path.startsWith("/api/v1/players/")) {
            handlePlayerDetail(exchange, svc, path);
        } else {
            sendError(exchange, 404, "Not found");
        }
    }

    private void handlePlayerList(HttpExchange exchange, PlayerDataService svc) throws IOException {
        try {
            Map<String, String> params = parseQueryParams(exchange);
            String search = params.get("search");

            List<PlayerData> players;
            if (search != null && !search.isBlank()) {
                players = svc.searchPlayers(search);
            } else {
                players = svc.getOnlinePlayers();
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("online_count", svc.getOnlinePlayers().size());
            data.put("players", players.stream().map(PlayerData::toSummaryMap).toList());
            sendJson(exchange, 200, data);
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to load players: " + e.getMessage());
        }
    }

    private void handlePlayerSearch(HttpExchange exchange, PlayerDataService svc, String path) throws IOException {
        try {
            Map<String, String> params = parseQueryParams(exchange);
            String query = params.get("q");
            if (query == null || query.isBlank()) {
                sendError(exchange, 400, "Missing search query");
                return;
            }

            List<PlayerData> results = svc.searchPlayers(query);
            sendJson(exchange, 200, results.stream().map(PlayerData::toSummaryMap).toList());
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to search players: " + e.getMessage());
        }
    }

    private void handlePlayerDetail(HttpExchange exchange, PlayerDataService svc, String path) throws IOException {
        try {
            String uuidStr = path.substring("/api/v1/players/".length());
            if (uuidStr.isEmpty()) {
                sendError(exchange, 400, "Missing player UUID");
                return;
            }

            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                sendError(exchange, 400, "Invalid UUID format");
                return;
            }

            PlayerData player = svc.getPlayer(uuid);
            if (player == null) {
                sendError(exchange, 404, "Player not found");
                return;
            }

            Map<String, Object> data = new LinkedHashMap<>(player.toMap());

            var moderationService = module.getPlugin().moderationService();
            if (moderationService != null) {
                var history = moderationService.getPlayerHistory(uuidStr, 1, 100);
                long activePunishments = history.getItems().stream()
                        .filter(r -> r.isActive() && !r.isExpired())
                        .count();
                long totalPunishments = history.getTotal();

                Map<String, Object> modSummary = new LinkedHashMap<>();
                modSummary.put("activePunishments", activePunishments);
                modSummary.put("totalPunishments", totalPunishments);
                data.put("moderation", modSummary);
            }

            sendJson(exchange, 200, data);
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to load player: " + e.getMessage());
        }
    }
}
