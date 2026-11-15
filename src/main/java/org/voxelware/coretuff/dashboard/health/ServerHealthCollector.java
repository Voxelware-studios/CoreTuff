package org.voxelware.coretuff.dashboard.health;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.voxelware.coretuff.CoreTuff;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ServerHealthCollector {

    private final CoreTuff plugin;

    public ServerHealthCollector(CoreTuff plugin) {
        this.plugin = plugin;
    }

    public HealthComponent collect() {
        Map<String, Object> details = new LinkedHashMap<>();
        HealthState state = HealthState.HEALTHY;
        StringBuilder message = new StringBuilder();

        Server server = Bukkit.getServer();

        double[] tps = getTps(server);
        details.put("tps", tps);
        details.put("tps_1m", tps[0]);
        details.put("tps_5m", tps[1]);
        details.put("tps_15m", tps[2]);

        double mspt = getMspt(server);
        details.put("mspt", mspt);

        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long usedMb = heap.getUsed() / 1024 / 1024;
        long maxMb = heap.getMax() / 1024 / 1024;
        double memPercent = maxMb > 0 ? (usedMb * 100.0 / maxMb) : 0;
        details.put("memory_used_mb", usedMb);
        details.put("memory_max_mb", maxMb);
        details.put("memory_percent", Math.round(memPercent * 10.0) / 10.0);

        double cpu = getCpuLoad();
        details.put("cpu_percent", Math.round(cpu * 10.0) / 10.0);

        int onlinePlayers = server.getOnlinePlayers().size();
        int maxPlayers = server.getMaxPlayers();
        details.put("online_players", onlinePlayers);
        details.put("max_players", maxPlayers);

        long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
        details.put("jvm_uptime_ms", jvmUptime);

        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        details.put("os", osName != null ? osName : "Unknown");
        details.put("arch", osArch != null ? osArch : "Unknown");
        details.put("java_version", System.getProperty("java.version"));
        details.put("java_vendor", System.getProperty("java.vendor"));

        int worldCount = server.getWorlds().size();
        details.put("worlds", worldCount);

        Map<String, Object> perfAlerts = new LinkedHashMap<>();
        perfAlerts.put("tps", Map.of(
                "status", tpsStatus(tps[0]),
                "value", tps[0],
                "warning", 19.0,
                "critical", 18.0
        ));
        perfAlerts.put("mspt", Map.of(
                "status", msptStatus(mspt),
                "value", mspt,
                "warning", 40.0,
                "critical", 80.0
        ));
        perfAlerts.put("memory", Map.of(
                "status", memoryStatus(memPercent),
                "value", memPercent,
                "warning", 80.0,
                "critical", 90.0
        ));
        if (cpu >= 0) {
            perfAlerts.put("cpu", Map.of(
                    "status", cpuStatus(cpu),
                    "value", cpu,
                    "warning", 80.0,
                    "critical", 95.0
            ));
        }
        details.put("performance_alerts", perfAlerts);
        details.put("performance_status", state.name());

        if (tps[0] < 15.0) {
            state = HealthState.CRITICAL;
            message.append("Severely low TPS (").append(String.format("%.1f", tps[0])).append(")");
        } else if (tps[0] < 18.0) {
            state = HealthState.WARNING;
            message.append("Low TPS (").append(String.format("%.1f", tps[0])).append(")");
        }

        if (mspt > 100) {
            state = state.merge(HealthState.CRITICAL);
            if (!message.isEmpty()) message.append("; ");
            message.append("Very high MSPT (").append(String.format("%.1f", mspt)).append("ms)");
        } else if (mspt > 50) {
            state = state.merge(HealthState.WARNING);
            if (!message.isEmpty()) message.append("; ");
            message.append("Elevated MSPT (").append(String.format("%.1f", mspt)).append("ms)");
        }

        if (memPercent > 95) {
            state = state.merge(HealthState.CRITICAL);
            if (!message.isEmpty()) message.append("; ");
            message.append("Memory critical (").append(String.format("%.0f", memPercent)).append("%)");
        } else if (memPercent > 80) {
            state = state.merge(HealthState.WARNING);
            if (!message.isEmpty()) message.append("; ");
            message.append("Memory pressure (").append(String.format("%.0f", memPercent)).append("%)");
        }

        if (state == HealthState.HEALTHY) {
            message.append("Server running normally");
        }

        return HealthComponent.builder("Server")
                .state(state)
                .message(message.toString())
                .detail("server_software", server.getName())
                .detail("server_version", Bukkit.getVersion())
                .detail("minecraft_version", Bukkit.getMinecraftVersion())
                .detail("bukkit_version", Bukkit.getBukkitVersion())
                .detail("folia", plugin.isFolia())
                .details(details)
                .build();
    }

    private double[] getTps(Server server) {
        try {
            return plugin.isFolia() ? new double[]{20.0, 20.0, 20.0} : server.getTPS();
        } catch (Exception e) {
            return new double[]{20.0, 20.0, 20.0};
        }
    }

    private double getMspt(Server server) {
        try {
            var method = server.getClass().getMethod("getAverageTickTime");
            Object result = method.invoke(server);
            return result instanceof Double d ? d : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private double getCpuLoad() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
                return sunBean.getProcessCpuLoad() * 100;
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private String tpsStatus(double tps) {
        if (tps < 18.0) return "CRITICAL";
        if (tps < 19.0) return "WARNING";
        return "HEALTHY";
    }

    private String msptStatus(double mspt) {
        if (mspt > 80.0) return "CRITICAL";
        if (mspt > 40.0) return "WARNING";
        return "HEALTHY";
    }

    private String memoryStatus(double mem) {
        if (mem > 90.0) return "CRITICAL";
        if (mem > 80.0) return "WARNING";
        return "HEALTHY";
    }

    private String cpuStatus(double cpu) {
        if (cpu > 95.0) return "CRITICAL";
        if (cpu > 80.0) return "WARNING";
        return "HEALTHY";
    }
}
