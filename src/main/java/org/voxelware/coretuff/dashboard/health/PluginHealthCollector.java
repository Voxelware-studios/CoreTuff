package org.voxelware.coretuff.dashboard.health;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.voxelware.coretuff.CoreTuff;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PluginHealthCollector {

    private final CoreTuff plugin;

    public PluginHealthCollector(CoreTuff plugin) {
        this.plugin = plugin;
    }

    public HealthComponent collect() {
        PluginManager pm = Bukkit.getPluginManager();
        Plugin[] plugins = pm.getPlugins();

        List<Map<String, Object>> pluginList = new ArrayList<>();
        int healthy = 0;
        int warning = 0;
        int error = 0;
        int disabled = 0;

        for (Plugin p : plugins) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", p.getName());

            PluginDescriptionFile desc = p.getDescription();
            info.put("version", desc.getVersion());
            info.put("enabled", p.isEnabled());
            info.put("description", desc.getDescription());
            info.put("authors", desc.getAuthors());
            info.put("website", desc.getWebsite());

            List<String> depend = desc.getDepend();
            List<String> softDepend = desc.getSoftDepend();
            if (depend != null && !depend.isEmpty()) {
                List<String> missing = new ArrayList<>();
                for (String dep : depend) {
                    Plugin depPlugin = pm.getPlugin(dep);
                    if (depPlugin == null || !depPlugin.isEnabled()) {
                        missing.add(dep);
                    }
                }
                if (!missing.isEmpty()) {
                    info.put("missing_dependencies", missing);
                }
            }
            if (softDepend != null && !softDepend.isEmpty()) {
                List<String> missingSoft = new ArrayList<>();
                for (String dep : softDepend) {
                    Plugin depPlugin = pm.getPlugin(dep);
                    if (depPlugin == null || !depPlugin.isEnabled()) {
                        missingSoft.add(dep);
                    }
                }
                if (!missingSoft.isEmpty()) {
                    info.put("missing_soft_dependencies", missingSoft);
                }
            }

            HealthState pluginState;
            String status;
            if (!p.isEnabled()) {
                pluginState = HealthState.WARNING;
                status = "DISABLED";
                disabled++;
                warning++;
            } else if (info.containsKey("missing_dependencies")) {
                pluginState = HealthState.WARNING;
                status = "WARNING";
                warning++;
            } else {
                pluginState = HealthState.HEALTHY;
                status = "HEALTHY";
                healthy++;
            }

            info.put("health_state", pluginState.name());
            info.put("status", status);
            pluginList.add(info);
        }

        HealthState overall;
        String message;
        if (error > 0) {
            overall = HealthState.CRITICAL;
            message = error + " plugin(s) in error state";
        } else if (warning > 0) {
            overall = HealthState.WARNING;
            message = warning + " plugin(s) with warnings, " + healthy + " healthy";
        } else {
            overall = HealthState.HEALTHY;
            message = healthy + " plugin(s) healthy";
        }

        return HealthComponent.builder("Plugins")
                .state(overall)
                .message(message)
                .detail("total", plugins.length)
                .detail("healthy", healthy)
                .detail("warning", warning)
                .detail("error", error)
                .detail("disabled", disabled)
                .detail("plugin_list", pluginList)
                .build();
    }
}
