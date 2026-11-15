package org.voxelware.coretuff.dashboard.health;

import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.dashboard.DashboardModule;
import org.voxelware.coretuff.editor.EditorModule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CoreTuffHealthCollector {

    private final CoreTuff plugin;

    public CoreTuffHealthCollector(CoreTuff plugin) {
        this.plugin = plugin;
    }

    public HealthComponent collect() {
        List<Map<String, Object>> subsystems = new ArrayList<>();
        HealthState overall = HealthState.HEALTHY;

        subsystems.add(checkDatabase());
        subsystems.add(checkConfiguration());
        subsystems.add(checkDashboard());
        subsystems.add(checkEditor());
        subsystems.add(checkModules());
        subsystems.add(checkExceptions());

        for (Map<String, Object> sub : subsystems) {
            String stateStr = (String) sub.get("state");
            if ("CRITICAL".equals(stateStr)) {
                overall = HealthState.CRITICAL;
                break;
            } else if ("WARNING".equals(stateStr)) {
                overall = HealthState.WARNING;
            }
        }

        String message;
        if (overall == HealthState.HEALTHY) {
            message = "All CoreTuff subsystems operational";
        } else if (overall == HealthState.WARNING) {
            message = "One or more subsystems have warnings";
        } else {
            message = "Critical subsystem failure detected";
        }

        return HealthComponent.builder("CoreTuff")
                .state(overall)
                .message(message)
                .detail("version", plugin.getDescription().getVersion())
                .detail("channel", plugin.getChannel())
                .detail("subsystems", subsystems)
                .build();
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", "Database");
        try {
            var conn = plugin.databaseManager();
            if (conn != null && conn.getConnection() != null && !conn.getConnection().isClosed()) {
                boolean valid = conn.getConnection().isValid(2);
                info.put("state", valid ? HealthState.HEALTHY.name() : HealthState.CRITICAL.name());
                info.put("message", valid ? "Connected" : "Connection invalid");
                info.put("connected", valid);
            } else {
                info.put("state", HealthState.CRITICAL.name());
                info.put("message", "Not connected");
                info.put("connected", false);
            }
        } catch (Exception e) {
            info.put("state", HealthState.CRITICAL.name());
            info.put("message", "Error: " + e.getMessage());
            info.put("connected", false);
        }
        return info;
    }

    private Map<String, Object> checkConfiguration() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", "Configuration");
        try {
            var cs = plugin.getConfigurationService();
            if (cs != null) {
                info.put("state", HealthState.HEALTHY.name());
                info.put("message", "Loaded");
                info.put("loaded", true);
            } else {
                info.put("state", HealthState.WARNING.name());
                info.put("message", "Not initialized");
                info.put("loaded", false);
            }
        } catch (Exception e) {
            info.put("state", HealthState.WARNING.name());
            info.put("message", "Error: " + e.getMessage());
            info.put("loaded", false);
        }
        return info;
    }

    private Map<String, Object> checkDashboard() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", "Dashboard");
        try {
            DashboardModule dm = plugin.getDashboardModule();
            if (dm == null) {
                info.put("state", HealthState.UNKNOWN.name());
                info.put("message", "Status unavailable");
                info.put("dashboard_state", "UNKNOWN");
                return info;
            }

            DashboardModule.DashboardState dashState = dm.getState();
            info.put("dashboard_state", dashState.name());

            switch (dashState) {
                case RUNNING -> {
                    info.put("state", HealthState.HEALTHY.name());
                    info.put("message", "Running on port " + dm.getPort());
                    info.put("running", true);
                    info.put("port", dm.getPort());
                    info.put("connected_clients", dm.getWebSocketManager() != null
                            ? dm.getWebSocketManager().getConnectedClients() : 0);
                }
                case STOPPED -> {
                    info.put("state", HealthState.WARNING.name());
                    info.put("message", "Stopped");
                    info.put("running", false);
                }
                case DISABLED -> {
                    info.put("state", HealthState.HEALTHY.name());
                    info.put("message", "Disabled in config");
                    info.put("running", false);
                }
                case ERROR -> {
                    info.put("state", HealthState.CRITICAL.name());
                    info.put("message", "Failed to start");
                    info.put("running", false);
                }
                case STARTING -> {
                    info.put("state", HealthState.WARNING.name());
                    info.put("message", "Starting");
                    info.put("running", false);
                }
                default -> {
                    info.put("state", HealthState.UNKNOWN.name());
                    info.put("message", "Status unavailable");
                    info.put("running", false);
                }
            }
        } catch (Exception e) {
            info.put("state", HealthState.CRITICAL.name());
            info.put("message", "Error: " + e.getMessage());
            info.put("running", false);
            info.put("dashboard_state", "ERROR");
        }
        return info;
    }

    private Map<String, Object> checkEditor() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", "Web Editor");
        try {
            EditorModule em = plugin.getEditorModule();
            if (em != null && em.isRunning()) {
                info.put("state", HealthState.HEALTHY.name());
                info.put("message", "Connected to backend");
                info.put("connected", true);
            } else {
                info.put("state", HealthState.WARNING.name());
                info.put("message", "Disconnected");
                info.put("connected", false);
            }
        } catch (Exception e) {
            info.put("state", HealthState.WARNING.name());
            info.put("message", "Error: " + e.getMessage());
            info.put("connected", false);
        }
        return info;
    }

    private Map<String, Object> checkModules() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", "Modules");

        int total = 0;
        int active = 0;
        List<Map<String, Object>> modules = new ArrayList<>();

        Map<String, Boolean> moduleMap = new LinkedHashMap<>();
        moduleMap.put("Economy", plugin.economyService() != null);
        moduleMap.put("Moderation", plugin.moderationService() != null);
        moduleMap.put("Warps", plugin.warpService() != null);
        moduleMap.put("Jails", plugin.jailService() != null);
        moduleMap.put("Kits", plugin.kitService() != null);

        for (Map.Entry<String, Boolean> entry : moduleMap.entrySet()) {
            total++;
            Map<String, Object> mod = new LinkedHashMap<>();
            mod.put("name", entry.getKey());
            mod.put("active", entry.getValue());
            if (entry.getValue()) active++;
            modules.add(mod);
        }

        info.put("total", total);
        info.put("active", active);
        info.put("module_list", modules);

        if (active == total) {
            info.put("state", HealthState.HEALTHY.name());
            info.put("message", active + "/" + total + " modules active");
        } else if (active > 0) {
            info.put("state", HealthState.WARNING.name());
            info.put("message", active + "/" + total + " modules active");
        } else {
            info.put("state", HealthState.CRITICAL.name());
            info.put("message", "No modules active");
        }

        return info;
    }

    private Map<String, Object> checkExceptions() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", "Exceptions");
        try {
            var buffer = plugin.exceptionBuffer();
            if (buffer != null) {
                var snapshot = buffer.snapshot();
                int count = snapshot.size();
                info.put("recent_count", count);
                info.put("max_buffer", 30);

                if (count > 10) {
                    info.put("state", HealthState.WARNING.name());
                    info.put("message", count + " recent exceptions");
                } else {
                    info.put("state", HealthState.HEALTHY.name());
                    info.put("message", count + " recent exception(s)");
                }

                if (!snapshot.isEmpty()) {
                    List<Map<String, Object>> recent = new ArrayList<>();
                    int limit = Math.min(5, snapshot.size());
                    for (int i = snapshot.size() - limit; i < snapshot.size(); i++) {
                        var record = snapshot.get(i);
                        Map<String, Object> ex = new LinkedHashMap<>();
                        ex.put("timestamp", record.timestamp().toEpochMilli());
                        ex.put("type", record.type());
                        ex.put("source", record.source());
                        ex.put("message", record.message());
                        recent.add(ex);
                    }
                    info.put("recent", recent);
                }
            } else {
                info.put("state", HealthState.UNKNOWN.name());
                info.put("message", "Buffer not available");
            }
        } catch (Exception e) {
            info.put("state", HealthState.WARNING.name());
            info.put("message", "Error checking exceptions");
        }
        return info;
    }
}
