package org.voxelware.coretuff.dashboard.health;

import org.voxelware.coretuff.CoreTuff;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HealthMonitor {

    private static final Logger LOG = Logger.getLogger(HealthMonitor.class.getName());
    private static final int COLLECT_INTERVAL_SECONDS = 10;

    private final CoreTuff plugin;
    private final ServerHealthCollector serverCollector;
    private final PluginHealthCollector pluginCollector;
    private final IntegrationHealthCollector integrationCollector;
    private final CoreTuffHealthCollector coretuffCollector;
    private final IncidentManager incidentManager;
    private final PerformanceMonitor performanceMonitor;

    private final List<HealthReport> history = new CopyOnWriteArrayList<>();
    private static final int MAX_HISTORY = 60;

    private volatile HealthReport lastReport;

    private final List<HealthChangeListener> listeners = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService scheduler;
    private volatile boolean running;
    private long startTime;

    public HealthMonitor(CoreTuff plugin) {
        this.plugin = plugin;
        this.serverCollector = new ServerHealthCollector(plugin);
        this.pluginCollector = new PluginHealthCollector(plugin);
        this.integrationCollector = new IntegrationHealthCollector(plugin);
        this.coretuffCollector = new CoreTuffHealthCollector(plugin);
        this.incidentManager = new IncidentManager(plugin);
        this.performanceMonitor = new PerformanceMonitor(plugin, incidentManager);
    }

    public void start() {
        if (running) return;
        running = true;
        startTime = System.currentTimeMillis();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ct-health-monitor");
            t.setDaemon(true);
            return t;
        });
        incidentManager.addListener((incident, action) -> {
            LOG.info("Incident " + action + ": " + incident.getId() + " - pushing immediate health update");
            pushImmediateUpdate();
        });
        collectAndReport();
        scheduler.scheduleAtFixedRate(this::collectAndReport,
                COLLECT_INTERVAL_SECONDS, COLLECT_INTERVAL_SECONDS, TimeUnit.SECONDS);
        LOG.info("Health monitor started (interval: " + COLLECT_INTERVAL_SECONDS + "s)");
    }

    public void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        LOG.info("Health monitor stopped.");
    }

    public void addListener(HealthChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(HealthChangeListener listener) {
        listeners.remove(listener);
    }

    public HealthReport getLastReport() {
        HealthReport report = lastReport;
        if (report == null) {
            return collectReport();
        }
        return report;
    }

    public List<HealthReport> getHistory() {
        return List.copyOf(history);
    }

    public HealthState getOverallState() {
        HealthReport report = lastReport;
        return report != null ? report.overallState() : HealthState.UNKNOWN;
    }

    public IncidentManager getIncidentManager() {
        return incidentManager;
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    public Map<String, Object> getServerInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("plugin_version", plugin.getDescription().getVersion());
        info.put("plugin_channel", plugin.getChannel());
        info.put("server_name", plugin.getServer().getName());
        info.put("minecraft_version", plugin.getServer().getMinecraftVersion());
        info.put("java_version", System.getProperty("java.version"));
        info.put("os_name", System.getProperty("os.name"));
        info.put("folia", plugin.isFolia());
        return info;
    }

    private void collectAndReport() {
        try {
            performanceMonitor.evaluate();

            HealthReport report = collectReport();
            lastReport = report;

            for (HealthChangeListener listener : listeners) {
                try {
                    listener.onHealthReport(report);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error in health listener", e);
                }
            }

            synchronized (history) {
                history.add(report);
                if (history.size() > MAX_HISTORY) {
                    history.removeFirst();
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to collect health report", e);
        }
    }

    private void pushImmediateUpdate() {
        if (!running) return;
        try {
            HealthReport report = collectReport();
            lastReport = report;
            for (HealthChangeListener listener : listeners) {
                try {
                    listener.onHealthReport(report);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error in immediate health listener", e);
                }
            }
            synchronized (history) {
                history.add(report);
                if (history.size() > MAX_HISTORY) {
                    history.removeFirst();
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to push immediate health update", e);
        }
    }

    private HealthReport collectReport() {
        List<HealthComponent> components = new ArrayList<>();

        try {
            components.add(serverCollector.collect());
        } catch (Exception e) {
            components.add(HealthComponent.builder("Server")
                    .state(HealthState.UNKNOWN)
                    .message("Collection failed: " + e.getMessage())
                    .build());
        }

        try {
            components.add(pluginCollector.collect());
        } catch (Exception e) {
            components.add(HealthComponent.builder("Plugins")
                    .state(HealthState.UNKNOWN)
                    .message("Collection failed: " + e.getMessage())
                    .build());
        }

        try {
            components.add(integrationCollector.collect());
        } catch (Exception e) {
            components.add(HealthComponent.builder("Integrations")
                    .state(HealthState.UNKNOWN)
                    .message("Collection failed: " + e.getMessage())
                    .build());
        }

        try {
            components.add(coretuffCollector.collect());
        } catch (Exception e) {
            components.add(HealthComponent.builder("CoreTuff")
                    .state(HealthState.UNKNOWN)
                    .message("Collection failed: " + e.getMessage())
                    .build());
        }

        HealthState overall = HealthState.HEALTHY;
        for (HealthComponent c : components) {
            overall = overall.merge(c.state());
        }

        List<Incident> activeIncidents = incidentManager.getActiveIncidents();
        for (Incident inc : activeIncidents) {
            if (inc.getSeverity() == IncidentSeverity.CRITICAL) {
                overall = HealthState.CRITICAL;
                break;
            } else if (inc.getSeverity() == IncidentSeverity.WARNING) {
                overall = overall.merge(HealthState.WARNING);
            }
        }

        long serverUptime = ManagementFactory.getRuntimeMXBean().getUptime();
        long monitorUptime = System.currentTimeMillis() - startTime;

        List<Map<String, Object>> recentIncidents = incidentManager.getRecentIncidentsMap(20);

        return new HealthReport(
                overall,
                System.currentTimeMillis(),
                serverUptime,
                monitorUptime,
                components,
                getServerInfo(),
                recentIncidents
        );
    }

    @FunctionalInterface
    public interface HealthChangeListener {
        void onHealthReport(HealthReport report);
    }
}
