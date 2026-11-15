package org.voxelware.coretuff.dashboard.health;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Incident {

    private final String id;
    private final IncidentType type;
    private final String title;
    private final String subsystem;
    private final String description;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private final long startedAt;
    private long resolvedAt;
    private final List<IncidentEvent> timeline;
    private final Map<String, Object> symptoms;
    private final Map<String, Object> currentValues;

    public Incident(String id, IncidentType type, String title, IncidentSeverity severity, String description) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.subsystem = type.name();
        this.description = description;
        this.severity = severity;
        this.status = IncidentStatus.DETECTED;
        this.startedAt = System.currentTimeMillis();
        this.resolvedAt = -1;
        this.timeline = new ArrayList<>();
        this.symptoms = new LinkedHashMap<>();
        this.currentValues = new LinkedHashMap<>();

        addEvent("DETECTED", description, severity);
        this.status = IncidentStatus.ACTIVE;
    }

    public synchronized void addEvent(String eventType, String message, IncidentSeverity eventSeverity) {
        timeline.add(new IncidentEvent(
                System.currentTimeMillis(), eventType, message, eventSeverity));
    }

    public synchronized void escalate(IncidentSeverity newSeverity, String reason) {
        if (newSeverity.isWorseThan(this.severity)) {
            this.severity = newSeverity;
            addEvent("ESCALATED", reason, newSeverity);
        }
    }

    public synchronized void addSymptom(String key, Object value) {
        symptoms.put(key, value);
    }

    public synchronized void updateValue(String key, Object value) {
        currentValues.put(key, value);
    }

    public synchronized void resolve(String reason) {
        this.status = IncidentStatus.RECOVERED;
        this.resolvedAt = System.currentTimeMillis();
        addEvent("RESOLVED", reason, null);
    }

    public boolean isResolved() {
        return status == IncidentStatus.RECOVERED;
    }

    public String getId() { return id; }
    public IncidentType getType() { return type; }
    public String getTitle() { return title; }
    public String getSubsystem() { return subsystem; }
    public String getDescription() { return description; }
    public IncidentSeverity getSeverity() { return severity; }
    public IncidentStatus getStatus() { return status; }
    public long getStartedAt() { return startedAt; }
    public long getResolvedAt() { return resolvedAt; }

    public synchronized List<IncidentEvent> getTimeline() {
        return List.copyOf(timeline);
    }

    public synchronized Map<String, Object> getSymptoms() {
        return Map.copyOf(symptoms);
    }

    public synchronized Map<String, Object> getCurrentValues() {
        return Map.copyOf(currentValues);
    }

    public synchronized Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("type", type.name());
        map.put("title", title);
        map.put("subsystem", subsystem);
        map.put("description", description);
        map.put("severity", severity.name());
        map.put("status", status.name());
        map.put("started_at", startedAt);
        map.put("resolved_at", resolvedAt);
        map.put("timeline", timeline.stream().map(IncidentEvent::toMap).toList());
        map.put("symptoms", Map.copyOf(symptoms));
        map.put("current_values", Map.copyOf(currentValues));
        return map;
    }
}
