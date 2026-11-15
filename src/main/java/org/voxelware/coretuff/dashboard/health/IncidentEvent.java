package org.voxelware.coretuff.dashboard.health;

import java.util.LinkedHashMap;
import java.util.Map;

public record IncidentEvent(
        long timestamp,
        String type,
        String message,
        IncidentSeverity severity
) {
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", timestamp);
        map.put("type", type);
        map.put("message", message);
        if (severity != null) {
            map.put("severity", severity.name());
        }
        return map;
    }
}
