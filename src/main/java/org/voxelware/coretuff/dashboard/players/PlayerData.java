package org.voxelware.coretuff.dashboard.players;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerData {

    private final UUID uuid;
    private final String name;
    private long firstSeen;
    private long lastSeen;
    private long playtime;
    private boolean online;
    private String world;
    private String gamemode;
    private int ping;
    private long sessionStart;

    public PlayerData(UUID uuid, String name, long firstSeen, long lastSeen, long playtime) {
        this.uuid = uuid;
        this.name = name;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
        this.playtime = playtime;
        this.online = false;
        this.world = "";
        this.gamemode = "SURVIVAL";
        this.ping = 0;
        this.sessionStart = 0;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public long getFirstSeen() { return firstSeen; }
    public long getLastSeen() { return lastSeen; }
    public long getPlaytime() { return playtime; }
    public boolean isOnline() { return online; }
    public String getWorld() { return world; }
    public String getGamemode() { return gamemode; }
    public int getPing() { return ping; }
    public long getSessionStart() { return sessionStart; }

    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    public void setPlaytime(long playtime) { this.playtime = playtime; }
    public void setOnline(boolean online) { this.online = online; }
    public void setWorld(String world) { this.world = world; }
    public void setGamemode(String gamemode) { this.gamemode = gamemode; }
    public void setPing(int ping) { this.ping = ping; }
    public void setSessionStart(long sessionStart) { this.sessionStart = sessionStart; }

    public void addPlaytime(long delta) {
        this.playtime += delta;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("uuid", uuid.toString());
        map.put("name", name);
        map.put("online", online);
        map.put("world", world);
        map.put("gamemode", gamemode);
        map.put("ping", ping);
        map.put("firstSeen", firstSeen);
        map.put("lastSeen", lastSeen);
        map.put("playtime", playtime);
        return map;
    }

    public Map<String, Object> toSummaryMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("uuid", uuid.toString());
        map.put("name", name);
        map.put("online", online);
        map.put("world", world);
        map.put("gamemode", gamemode);
        map.put("ping", ping);
        map.put("lastSeen", lastSeen);
        return map;
    }
}
