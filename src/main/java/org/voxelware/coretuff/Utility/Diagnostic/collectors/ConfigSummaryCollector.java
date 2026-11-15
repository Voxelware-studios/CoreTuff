package org.voxelware.coretuff.Utility.Diagnostic.collectors;

import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.Utility.Diagnostic.DiagnosticCollector;
import org.voxelware.coretuff.Utility.Diagnostic.DiagnosticDumpService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigSummaryCollector implements DiagnosticCollector {

    private final CoreTuff plugin;

    public ConfigSummaryCollector(CoreTuff plugin) {
        this.plugin = plugin;
    }

    @Override
    public String sectionName() {
        return "Configuration Summary";
    }

    @Override
    public void collect(DiagnosticDumpService service, List<String> lines) {
        Map<String, String> settings = new LinkedHashMap<>();

        try {
            var security = plugin.getSecurityConfig();
            settings.put("Plugin Hide", security != null && security.pluginHideEnabled() ? "Enabled" : "Disabled");
        } catch (Exception e) {
            settings.put("Plugin Hide", "Unavailable");
        }

        try {
            var eco = plugin.getEconomyConfig();
            settings.put("Economy", eco != null ? "Enabled" : "Disabled");
            settings.put("Interest", eco != null && eco.interestEnabled() ? "Enabled" : "Disabled");
            settings.put("Taxes", eco != null && eco.payTaxPercent() > 0 ? eco.payTaxPercent() + "%" : "Disabled");
        } catch (Exception e) {
            settings.put("Economy", "Unavailable");
            settings.put("Interest", "Unavailable");
            settings.put("Taxes", "Unavailable");
        }

        try {
            var land = plugin.getLandConfig();
            settings.put("Max homes", land != null ? String.valueOf(land.maxHomesDefault()) : "Unavailable");
            settings.put("Max warps", "N/A");
        } catch (Exception e) {
            settings.put("Max homes", "Unavailable");
            settings.put("Max warps", "Unavailable");
        }

        try {
            var teleport = plugin.getTeleportConfig();
            settings.put("Teleport cooldown", teleport != null ? teleport.get().getInt("Teleportation.tpCooldown", 200) + "s" : "Unavailable");
        } catch (Exception e) {
            settings.put("Teleport cooldown", "Unavailable");
        }

        settings.put("Debug mode", "Disabled");
        settings.put("Auto-update checker", "Enabled");

        for (Map.Entry<String, String> entry : settings.entrySet()) {
            lines.add("  " + pad(entry.getKey(), 28) + ": " + entry.getValue());
        }
    }

    private String pad(String s, int len) {
        if (s.length() >= len) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < len) sb.append(" ");
        return sb.toString();
    }
}
