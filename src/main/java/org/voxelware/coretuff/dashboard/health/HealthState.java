package org.voxelware.coretuff.dashboard.health;

public enum HealthState {
    HEALTHY,
    WARNING,
    CRITICAL,
    UNKNOWN;

    public boolean isWorseThan(HealthState other) {
        return this.ordinal() > other.ordinal();
    }

    public HealthState merge(HealthState other) {
        if (this == CRITICAL || other == CRITICAL) return CRITICAL;
        if (this == WARNING || other == WARNING) return WARNING;
        if (this == UNKNOWN && other == UNKNOWN) return UNKNOWN;
        if (this == UNKNOWN) return other;
        if (other == UNKNOWN) return this;
        return HEALTHY;
    }
}
