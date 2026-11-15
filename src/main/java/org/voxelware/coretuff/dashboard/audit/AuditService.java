package org.voxelware.coretuff.dashboard.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class AuditService {

    private static final int MAX_ENTRIES = 1000;
    private final List<AuditEntry> entries = Collections.synchronizedList(new ArrayList<>());

    public record AuditEntry(
            long timestamp,
            String actor,
            String module,
            String setting,
            Object oldValue,
            Object newValue,
            String ipAddress,
            String reason
    ) {}

    public void log(String actor, String module, String setting, Object oldValue, Object newValue,
                    String ipAddress, String reason) {
        AuditEntry entry = new AuditEntry(
                System.currentTimeMillis(),
                actor,
                module,
                setting,
                oldValue,
                newValue,
                ipAddress,
                reason
        );
        synchronized (entries) {
            if (entries.size() >= MAX_ENTRIES) {
                entries.removeFirst();
            }
            entries.add(entry);
        }
    }

    public List<AuditEntry> getEntries() {
        synchronized (entries) {
            return List.copyOf(entries);
        }
    }

    public List<AuditEntry> getEntries(String module) {
        synchronized (entries) {
            return entries.stream()
                    .filter(e -> e.module().equals(module))
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    public List<AuditEntry> searchEntries(String query) {
        if (query == null || query.isBlank()) return getEntries();
        String lower = query.toLowerCase();
        synchronized (entries) {
            return entries.stream()
                    .filter(e -> e.actor().toLowerCase().contains(lower)
                            || e.module().toLowerCase().contains(lower)
                            || e.setting().toLowerCase().contains(lower)
                            || e.reason().toLowerCase().contains(lower)
                            || String.valueOf(e.oldValue()).toLowerCase().contains(lower)
                            || String.valueOf(e.newValue()).toLowerCase().contains(lower))
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    public List<AuditEntry> getRecent(int count) {
        synchronized (entries) {
            int size = entries.size();
            int from = Math.max(0, size - count);
            return List.copyOf(entries.subList(from, size));
        }
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        synchronized (entries) {
            entries.clear();
        }
    }
}
