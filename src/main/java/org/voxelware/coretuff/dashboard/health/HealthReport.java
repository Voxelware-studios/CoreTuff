package org.voxelware.coretuff.dashboard.health;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record HealthReport(
        HealthState overallState,
        long timestamp,
        long serverUptime,
        long monitorUptime,
        List<HealthComponent> components,
        Map<String, Object> serverInfo,
        List<Map<String, Object>> incidents
) {
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("overall_state", overallState.name());
        map.put("timestamp", timestamp);
        map.put("server_uptime", serverUptime);
        map.put("monitor_uptime", monitorUptime);

        List<Map<String, Object>> componentList = components.stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", c.name());
                    m.put("state", c.state().name());
                    m.put("message", c.message());
                    m.put("details", c.details());
                    return m;
                })
                .toList();
        map.put("components", componentList);
        map.put("server_info", serverInfo);
        map.put("incidents", incidents != null ? incidents : List.of());
        return map;
    }
}
