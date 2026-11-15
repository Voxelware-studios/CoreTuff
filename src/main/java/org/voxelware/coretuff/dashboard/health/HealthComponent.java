package org.voxelware.coretuff.dashboard.health;

import java.util.LinkedHashMap;
import java.util.Map;

public record HealthComponent(
        String name,
        HealthState state,
        String message,
        Map<String, Object> details
) {
    public HealthComponent {
        details = details != null ? Map.copyOf(details) : Map.of();
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private HealthState state = HealthState.UNKNOWN;
        private String message = "";
        private final Map<String, Object> details = new LinkedHashMap<>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder state(HealthState state) {
            this.state = state;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder detail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }

        public Builder details(Map<String, Object> map) {
            if (map != null) this.details.putAll(map);
            return this;
        }

        public HealthComponent build() {
            return new HealthComponent(name, state, message, details);
        }
    }
}
