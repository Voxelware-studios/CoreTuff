package org.voxelware.coretuff.dashboard.health;

public enum IncidentSeverity {
    INFO,
    WARNING,
    CRITICAL;

    public boolean isWorseThan(IncidentSeverity other) {
        return this.ordinal() > other.ordinal();
    }
}
