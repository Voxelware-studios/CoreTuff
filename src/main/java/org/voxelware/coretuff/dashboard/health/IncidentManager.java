package org.voxelware.coretuff.dashboard.health;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.voxelware.coretuff.CoreTuff;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public final class IncidentManager {

    private static final Logger LOG = Logger.getLogger(IncidentManager.class.getName());
    private static final int MAX_INCIDENTS = 500;

    private final CoreTuff plugin;
    private final AtomicInteger idCounter = new AtomicInteger(1000);
    private final List<Incident> incidents = new CopyOnWriteArrayList<>();
    private final List<IncidentListener> listeners = new CopyOnWriteArrayList<>();

    public IncidentManager(CoreTuff plugin) {
        this.plugin = plugin;
    }

    public Incident createIncident(IncidentType type, String title, IncidentSeverity severity, String description) {
        Optional<Incident> existing = findActiveByTypeAndTitle(type, title);
        if (existing.isPresent()) {
            Incident incident = existing.get();
            incident.addEvent("UPDATED", description, severity);
            if (severity.isWorseThan(incident.getSeverity())) {
                incident.escalate(severity, "Escalated to " + severity.name());
            }
            LOG.info("Incident updated (dedup): " + incident.getId() + " [" + severity.name() + "] " + title);
            notifyListeners(incident, "updated");
            return incident;
        }

        String id = "INC-" + idCounter.incrementAndGet();
        Incident incident = new Incident(id, type, title, severity, description);
        incidents.add(incident);
        trimIncidents();
        LOG.info("Incident created: " + id + " [" + severity.name() + "] " + title);
        notifyListeners(incident, "created");
        notifyPlayers(incident);
        return incident;
    }

    public Optional<Incident> findActiveByType(IncidentType type) {
        return incidents.stream()
                .filter(i -> i.getType() == type && !i.isResolved())
                .findFirst();
    }

    public Optional<Incident> findActiveByTypeAndTitle(IncidentType type, String title) {
        return incidents.stream()
                .filter(i -> i.getType() == type && !i.isResolved() && i.getTitle().equals(title))
                .findFirst();
    }

    public void escalateIncident(Incident incident, IncidentSeverity newSeverity, String reason) {
        if (!incident.isResolved() && newSeverity.isWorseThan(incident.getSeverity())) {
            incident.escalate(newSeverity, reason);
            LOG.info("Incident escalated: " + incident.getId() + " " + incident.getSeverity().name() + " -> " + newSeverity.name());
            notifyListeners(incident, "escalated");
        }
    }

    public void resolveIncident(Incident incident, String reason) {
        if (!incident.isResolved()) {
            incident.resolve(reason);
            LOG.info("Incident resolved: " + incident.getId() + " - " + reason);
            notifyListeners(incident, "resolved");
        }
    }

    public List<Incident> getActiveIncidents() {
        return incidents.stream()
                .filter(i -> !i.isResolved())
                .sorted(Comparator.comparingLong(Incident::getStartedAt).reversed())
                .toList();
    }

    public List<Incident> getRecentIncidents(int limit) {
        return incidents.stream()
                .sorted(Comparator.comparingLong(Incident::getStartedAt).reversed())
                .limit(limit)
                .toList();
    }

    public List<Map<String, Object>> getRecentIncidentsMap(int limit) {
        return getRecentIncidents(limit).stream()
                .map(Incident::toMap)
                .toList();
    }

    public List<Incident> getAllIncidents() {
        return List.copyOf(incidents);
    }

    public void addListener(IncidentListener listener) {
        listeners.add(listener);
    }

    public void removeListener(IncidentListener listener) {
        listeners.remove(listener);
    }

    private void notifyPlayers(Incident incident) {
        if (plugin.getServer() == null) return;
        String message = "[CoreTuff] [" + incident.getSeverity().name() + "] " + incident.getTitle();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("coretuff.admin.notifyincident")) {
                player.sendMessage(message);
            }
        }
    }

    private void trimIncidents() {
        while (incidents.size() > MAX_INCIDENTS) {
            boolean removed = incidents.stream()
                    .filter(Incident::isResolved)
                    .min(Comparator.comparingLong(Incident::getStartedAt))
                    .map(incidents::remove)
                    .isPresent();
            if (!removed) {
                incidents.removeFirst();
            }
        }
    }

    private void notifyListeners(Incident incident, String action) {
        for (IncidentListener listener : listeners) {
            try {
                listener.onIncident(incident, action);
            } catch (Exception e) {
                LOG.warning("Error in incident listener: " + e.getMessage());
            }
        }
    }

    @FunctionalInterface
    public interface IncidentListener {
        void onIncident(Incident incident, String action);
    }
}
