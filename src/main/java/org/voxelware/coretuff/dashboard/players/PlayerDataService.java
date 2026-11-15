package org.voxelware.coretuff.dashboard.players;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.database.DatabaseManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class PlayerDataService implements Listener {

    private static final Logger LOG = Logger.getLogger(PlayerDataService.class.getName());
    private static final int PING_SYNC_INTERVAL_SECONDS = 10;
    private static final int SNAPSHOT_INTERVAL_SECONDS = 10;

    private final CoreTuff plugin;
    private final PlayerDataRepository repository;
    private final ConcurrentHashMap<UUID, PlayerData> onlinePlayers = new ConcurrentHashMap<>();
    private ScheduledExecutorService pingScheduler;
    private volatile boolean running;

    public PlayerDataService(CoreTuff plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.repository = new PlayerDataRepository(databaseManager.getConnection());
    }

    public void start() {
        if (running) return;
        running = true;

        for (Player player : Bukkit.getOnlinePlayers()) {
            trackPlayer(player);
        }

        pingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ct-player-ping");
            t.setDaemon(true);
            return t;
        });
        pingScheduler.scheduleAtFixedRate(this::syncPingData,
                PING_SYNC_INTERVAL_SECONDS, PING_SYNC_INTERVAL_SECONDS, TimeUnit.SECONDS);
        pingScheduler.scheduleAtFixedRate(this::broadcastPlayerSnapshot,
                SNAPSHOT_INTERVAL_SECONDS, SNAPSHOT_INTERVAL_SECONDS, TimeUnit.SECONDS);

        LOG.info("PlayerDataService started. Tracking " + onlinePlayers.size() + " online players.");
    }

    public void stop() {
        running = false;
        persistAllPlaytime();
        if (pingScheduler != null) {
            pingScheduler.shutdown();
        }
        onlinePlayers.clear();
        LOG.info("PlayerDataService stopped.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!running) return;
        Player player = event.getPlayer();
        trackPlayer(player);
        broadcastPlayerUpdate("join", player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!running) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PlayerData data = onlinePlayers.get(uuid);
        if (data != null) {
            long now = System.currentTimeMillis();
            if (data.getSessionStart() > 0) {
                data.addPlaytime(now - data.getSessionStart());
            }
            data.setLastSeen(now);
            data.setOnline(false);
            repository.upsert(uuid, data.getName(), data.getFirstSeen(), data.getLastSeen(), data.getPlaytime());
        }

        onlinePlayers.remove(uuid);
        broadcastPlayerUpdate("leave", uuid);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (!running) return;
        Player player = event.getPlayer();
        PlayerData data = onlinePlayers.get(player.getUniqueId());
        if (data != null) {
            data.setWorld(player.getWorld().getName());
            broadcastPlayerUpdate("world_change", player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        if (!running) return;
        Player player = event.getPlayer();
        PlayerData data = onlinePlayers.get(player.getUniqueId());
        if (data != null) {
            data.setGamemode(event.getNewGameMode().name());
            broadcastPlayerUpdate("gamemode_change", player.getUniqueId());
        }
    }

    private void trackPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        PlayerData existing = repository.getByUuid(uuid);
        if (existing != null) {
            existing.setOnline(true);
            existing.setWorld(player.getWorld().getName());
            existing.setGamemode(player.getGameMode().name());
            existing.setPing(player.getPing());
            existing.setLastSeen(now);
            existing.setSessionStart(now);
            onlinePlayers.put(uuid, existing);
            repository.updateLastSeen(uuid, now);
        } else {
            PlayerData data = new PlayerData(uuid, player.getName(), now, now, 0);
            data.setOnline(true);
            data.setWorld(player.getWorld().getName());
            data.setGamemode(player.getGameMode().name());
            data.setPing(player.getPing());
            data.setSessionStart(now);
            onlinePlayers.put(uuid, data);
            repository.upsert(uuid, player.getName(), now, now, 0);
        }
    }

    private void syncPingData() {
        if (!running) return;
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = onlinePlayers.get(player.getUniqueId());
                if (data != null) {
                    data.setPing(player.getPing());
                }
            }
        } catch (Exception e) {
            LOG.warning("Failed to sync ping data: " + e.getMessage());
        }
    }

    private void persistAllPlaytime() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, PlayerData> entry : onlinePlayers.entrySet()) {
            PlayerData data = entry.getValue();
            if (data.getSessionStart() > 0) {
                data.addPlaytime(now - data.getSessionStart());
            }
            data.setLastSeen(now);
            repository.upsert(entry.getKey(), data.getName(), data.getFirstSeen(), data.getLastSeen(), data.getPlaytime());
        }
    }

    public List<PlayerData> getOnlinePlayers() {
        return new ArrayList<>(onlinePlayers.values());
    }

    public PlayerData getPlayer(UUID uuid) {
        PlayerData online = onlinePlayers.get(uuid);
        if (online != null) return online;
        return repository.getByUuid(uuid);
    }

    public List<PlayerData> searchPlayers(String query) {
        List<PlayerData> onlineResults = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (PlayerData data : onlinePlayers.values()) {
            if (data.getName().toLowerCase().contains(lowerQuery)
                    || data.getUuid().toString().toLowerCase().contains(lowerQuery)) {
                onlineResults.add(data);
            }
        }
        if (!onlineResults.isEmpty()) {
            return onlineResults;
        }
        return repository.search(query, 50);
    }

    public Map<String, Object> getPlayerSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("online_count", onlinePlayers.size());
        List<Map<String, Object>> players = new ArrayList<>();
        for (PlayerData data : onlinePlayers.values()) {
            players.add(data.toSummaryMap());
        }
        snapshot.put("players", players);
        return snapshot;
    }

    private void broadcastPlayerUpdate(String action, UUID uuid) {
        CoreTuff plugin = this.plugin;
        if (plugin == null) return;

        var editorModule = plugin.getEditorModule();
        if (editorModule == null || !editorModule.isRunning()) return;

        PlayerData data = onlinePlayers.get(uuid);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", action);
        payload.put("player", data != null ? data.toMap() : Map.of("uuid", uuid.toString()));

        com.google.gson.JsonObject message = new com.google.gson.JsonObject();
        message.addProperty("type", "player_update");
        message.add("payload", new com.google.gson.Gson().toJsonTree(payload));
        message.addProperty("timestamp", System.currentTimeMillis());

        editorModule.sendMessage(message);
    }

    private void broadcastPlayerSnapshot() {
        if (!running) return;
        CoreTuff plugin = this.plugin;
        if (plugin == null) return;
        var editorModule = plugin.getEditorModule();
        if (editorModule == null || !editorModule.isRunning()) return;
        editorModule.broadcastPlayerSnapshot();
    }

    public PlayerDataRepository getRepository() {
        return repository;
    }
}
