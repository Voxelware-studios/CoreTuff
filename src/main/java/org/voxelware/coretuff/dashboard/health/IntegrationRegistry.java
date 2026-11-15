package org.voxelware.coretuff.dashboard.health;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class IntegrationRegistry {

    private static final Logger LOG = Logger.getLogger(IntegrationRegistry.class.getName());

    private final Map<String, Integration> integrations = new ConcurrentHashMap<>();

    public void register(Integration integration) {
        if (integration == null || integration.id() == null || integration.id().isBlank()) {
            LOG.warning("Attempted to register integration with null/blank id");
            return;
        }
        Integration existing = integrations.put(integration.id(), integration);
        if (existing != null) {
            LOG.info("Integration updated: " + integration.id());
        } else {
            LOG.info("Integration registered: " + integration.id());
        }
    }

    public void unregister(String id) {
        if (id != null && integrations.remove(id) != null) {
            LOG.info("Integration unregistered: " + id);
        }
    }

    public Optional<Integration> get(String id) {
        return Optional.ofNullable(integrations.get(id));
    }

    public List<Integration> getAll() {
        return List.copyOf(integrations.values());
    }

    public int getCount() {
        return integrations.size();
    }

    public boolean isRegistered(String id) {
        return integrations.containsKey(id);
    }

    public List<Map<String, Object>> toList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Integration integration : integrations.values()) {
            list.add(integration.toMap());
        }
        return Collections.unmodifiableList(list);
    }

    public record Integration(
            String id,
            String name,
            String provider,
            String version,
            IntegrationStatus status,
            String description,
            List<String> capabilities,
            Map<String, Object> metadata
    ) {
        public Integration {
            capabilities = capabilities != null ? List.copyOf(capabilities) : List.of();
            metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("name", name);
            map.put("provider", provider);
            map.put("version", version);
            map.put("status", status.name());
            map.put("description", description);
            map.put("capabilities", capabilities);
            if (!metadata.isEmpty()) {
                map.put("metadata", metadata);
            }
            return map;
        }
    }

    public enum IntegrationStatus {
        ACTIVE,
        DEGRADED,
        ERROR
    }
}
