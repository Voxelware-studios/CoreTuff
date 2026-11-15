package org.voxelware.coretuff.Utility.Diagnostic.collectors;

import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.Utility.Diagnostic.DiagnosticCollector;
import org.voxelware.coretuff.Utility.Diagnostic.DiagnosticDumpService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigVersionCollector implements DiagnosticCollector {

    private final CoreTuff plugin;

    public ConfigVersionCollector(CoreTuff plugin) {
        this.plugin = plugin;
    }

    @Override
    public String sectionName() {
        return "Configuration Database";
    }

    @Override
    public void collect(DiagnosticDumpService service, List<String> lines) {
        var cfgService = plugin.getConfigurationService();
        if (cfgService == null) {
            lines.add("  Status: Not initialized");
            return;
        }

        var all = cfgService.getAll();
        int totalEntries = 0;
        for (var moduleEntry : all.entrySet()) {
            totalEntries += moduleEntry.getValue().size();
        }
        lines.add("  Modules: " + all.size());
        lines.add("  Total entries: " + totalEntries);
        for (var moduleEntry : all.entrySet()) {
            lines.add("    " + pad(moduleEntry.getKey(), 16) + ": " + moduleEntry.getValue().size() + " settings");
        }
    }

    private String pad(String s, int len) {
        if (s.length() >= len) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < len) sb.append(" ");
        return sb.toString();
    }
}
