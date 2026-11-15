package org.voxelware.coretuff.dashboard.metrics;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.voxelware.coretuff.CoreTuff;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class MetricsCollector {

    private static final Logger LOG = Logger.getLogger(MetricsCollector.class.getName());
    private static final int SAMPLE_INTERVAL_MS = 5000;
    private static final int HISTORY_MINUTES = 5;
    private static final int HISTORY_SIZE = (HISTORY_MINUTES * 60_000) / SAMPLE_INTERVAL_MS;

    private final CoreTuff plugin;
    private final List<MetricsSnapshot> history = new ArrayList<>();
    private final Object historyLock = new Object();

    private volatile int cachedTotalChunks;
    private volatile int cachedTotalEntities;
    private volatile long lastSyncTimestamp;

    private ScheduledExecutorService scheduler;
    private volatile boolean running;

    public MetricsCollector(CoreTuff plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (running) return;
        running = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ct-metrics");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::collectSample, 0, SAMPLE_INTERVAL_MS, TimeUnit.MILLISECONDS);
        scheduleSyncWorldData();
    }

    private void scheduleSyncWorldData() {
        if (plugin.isFolia()) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> syncWorldData(), 1L, SAMPLE_INTERVAL_MS / 50L);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, this::syncWorldData, 1L, SAMPLE_INTERVAL_MS / 50L);
        }
    }

    private void syncWorldData() {
        try {
            int chunks = 0;
            int entities = 0;
            for (World world : Bukkit.getServer().getWorlds()) {
                chunks += world.getLoadedChunks().length;
                entities += world.getEntities().size();
            }
            this.cachedTotalChunks = chunks;
            this.cachedTotalEntities = entities;
            this.lastSyncTimestamp = System.currentTimeMillis();
        } catch (Exception e) {
            LOG.warning("Failed to sync world data: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    public Map<String, Object> getCurrent() {
        MetricsSnapshot snapshot = collectSnapshot();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tps", plugin.isFolia() ? new double[]{20.0, 20.0, 20.0} : Bukkit.getTPS());
        result.put("mspt", snapshot.mspt);
        result.put("memory", memoryMap(snapshot.memoryUsage));
        result.put("cpu", snapshot.cpuUsage);
        result.put("players", Map.of(
                "online", snapshot.onlinePlayers,
                "max", snapshot.maxPlayers
        ));
        result.put("worlds", Map.of(
                "count", snapshot.worldCount,
                "chunks", snapshot.totalChunks,
                "entities", snapshot.totalEntities
        ));
        result.put("uptime", snapshot.uptime);
        result.put("timestamp", snapshot.timestamp);
        return result;
    }

    public List<Map<String, Object>> getHistory(int minutes) {
        int count = (minutes * 60_000) / SAMPLE_INTERVAL_MS;
        synchronized (historyLock) {
            int size = history.size();
            int from = Math.max(0, size - Math.min(count, size));
            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = from; i < size; i++) {
                MetricsSnapshot s = history.get(i);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("timestamp", s.timestamp);
                m.put("tps", s.tps);
                m.put("mspt", s.mspt);
                m.put("memory", s.memoryUsage.getUsed() / 1024.0 / 1024.0);
                m.put("cpu", s.cpuUsage);
                m.put("players", s.onlinePlayers);
                result.add(m);
            }
            return result;
        }
    }

    private void collectSample() {
        try {
            MetricsSnapshot snapshot = collectSnapshot();
            synchronized (historyLock) {
                history.add(snapshot);
                if (history.size() > HISTORY_SIZE) {
                    history.removeFirst();
                }
            }
        } catch (Exception e) {
            LOG.warning("Failed to collect metrics: " + e.getMessage());
        }
    }

    private MetricsSnapshot collectSnapshot() {
        Server server = Bukkit.getServer();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

        double[] tps;
        try {
            tps = plugin.isFolia() ? new double[]{20.0, 20.0, 20.0} : server.getTPS();
        } catch (Exception e) {
            tps = new double[]{20.0, 20.0, 20.0};
        }

        double mspt = 0;
        try {
            var msptField = server.getClass().getMethod("getAverageTickTime");
            mspt = (double) msptField.invoke(server);
        } catch (Exception ignored) {}

        MemoryUsage heapUsage = memBean.getHeapMemoryUsage();

        double cpuLoad = 0;
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            cpuLoad = sunOsBean.getProcessCpuLoad() * 100;
        }

        int onlinePlayers = server.getOnlinePlayers().size();
        int maxPlayers = server.getMaxPlayers();
        int worldCount = server.getWorlds().size();

        int totalChunks = this.cachedTotalChunks;
        int totalEntities = this.cachedTotalEntities;

        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();

        return new MetricsSnapshot(
                System.currentTimeMillis(),
                tps, mspt, heapUsage, cpuLoad,
                onlinePlayers, maxPlayers,
                worldCount, totalChunks, totalEntities, uptime
        );
    }

    private Map<String, Object> memoryMap(MemoryUsage usage) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("used", usage.getUsed() / 1024.0 / 1024.0);
        m.put("max", usage.getMax() / 1024.0 / 1024.0);
        m.put("committed", usage.getCommitted() / 1024.0 / 1024.0);
        m.put("init", usage.getInit() / 1024.0 / 1024.0);
        m.put("usedPercent", usage.getMax() > 0 ? (usage.getUsed() * 100.0 / usage.getMax()) : 0);
        return m;
    }

    private record MetricsSnapshot(
            long timestamp,
            double[] tps,
            double mspt,
            MemoryUsage memoryUsage,
            double cpuUsage,
            int onlinePlayers,
            int maxPlayers,
            int worldCount,
            int totalChunks,
            int totalEntities,
            long uptime
    ) {}
}
