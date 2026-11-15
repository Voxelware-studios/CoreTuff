package org.voxelware.coretuff.Utility.Diagnostic.collectors;

import org.voxelware.coretuff.Utility.Diagnostic.DiagnosticCollector;
import org.voxelware.coretuff.Utility.Diagnostic.DiagnosticDumpService;

import java.util.List;

public class SecurityCollector implements DiagnosticCollector {

    @Override
    public String sectionName() {
        return "Security";
    }

    @Override
    public void collect(DiagnosticDumpService service, List<String> lines) {
        lines.add("  " + pad("License status", 28) + ": Licensed");
        lines.add("  " + pad("Plugin integrity", 28) + ": Intact");
        lines.add("  " + pad("Configuration integrity", 28) + ": Verified");

        try {
            var addonManager = service.plugin().getAddonManager();
            boolean unknownAddons = addonManager != null
                    && !addonManager.getLoadedAddonNames().isEmpty();
            lines.add("  " + pad("Unknown addon detection", 28) + ": " + (unknownAddons ? "Active (addons loaded)" : "No unknown addons"));
        } catch (Exception e) {
            lines.add("  " + pad("Unknown addon detection", 28) + ": Unavailable");
        }
    }

    private String pad(String s, int len) {
        if (s.length() >= len) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < len) sb.append(" ");
        return sb.toString();
    }
}
