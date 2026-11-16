package org.voxelware.coretuff.utility.diagnostic.collectors;

import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.utility.diagnostic.DiagnosticCollector;
import org.voxelware.coretuff.utility.diagnostic.DiagnosticDumpService;

import java.util.List;

public class AddonCollector implements DiagnosticCollector {

    private final CoreTuff plugin;

    public AddonCollector(CoreTuff plugin) {
        this.plugin = plugin;
    }

    @Override
    public String sectionName() {
        return "Loaded Addons";
    }

    @Override
    public void collect(DiagnosticDumpService service, List<String> lines) {
        try {
            var addonManager = plugin.getAddonManager();
            if (addonManager == null) {
                lines.add("  No addon loader available.");
                return;
            }

            List<String> addons = addonManager.getLoadedAddonNames();
            if (addons.isEmpty()) {
                lines.add("  No addons loaded.");
            } else {
                for (String name : addons) {
                    lines.add("  " + name);
                }
            }
        } catch (Exception e) {
            lines.add("  Unavailable");
        }
    }
}
