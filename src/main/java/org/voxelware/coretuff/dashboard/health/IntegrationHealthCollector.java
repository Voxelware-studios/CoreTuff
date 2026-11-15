package org.voxelware.coretuff.dashboard.health;

import org.voxelware.coretuff.CoreTuff;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class IntegrationHealthCollector {

    private final CoreTuff plugin;

    public IntegrationHealthCollector(CoreTuff plugin) {
        this.plugin = plugin;
    }

    public HealthComponent collect() {
        IntegrationRegistry registry = plugin.getIntegrationRegistry();
        if (registry == null) {
            return HealthComponent.builder("Integrations")
                    .state(HealthState.UNKNOWN)
                    .message("Integration registry not available")
                    .detail("total", 0)
                    .detail("active", 0)
                    .detail("integration_list", List.of())
                    .build();
        }

        List<Map<String, Object>> integrationList = registry.toList();
        int active = 0;
        int degraded = 0;
        int error = 0;

        for (Map<String, Object> entry : integrationList) {
            String status = (String) entry.get("status");
            if ("ACTIVE".equals(status)) active++;
            else if ("DEGRADED".equals(status)) degraded++;
            else if ("ERROR".equals(status)) error++;
        }

        int total = integrationList.size();
        HealthState state;
        String message;

        if (total == 0) {
            state = HealthState.HEALTHY;
            message = "No integrations registered";
        } else if (error > 0) {
            state = HealthState.CRITICAL;
            message = error + " integration(s) in error state";
        } else if (degraded > 0) {
            state = HealthState.WARNING;
            message = active + " active, " + degraded + " degraded";
        } else {
            state = HealthState.HEALTHY;
            message = active + " active";
        }

        return HealthComponent.builder("Integrations")
                .state(state)
                .message(message)
                .detail("total", total)
                .detail("active", active)
                .detail("integration_list", integrationList)
                .build();
    }
}
