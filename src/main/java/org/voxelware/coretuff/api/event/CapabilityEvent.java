package org.voxelware.coretuff.api.event;

import org.voxelware.coretuff.api.addon.Capability;
import org.voxelware.coretuff.api.addon.TrustLevel;

public class CapabilityEvent extends CoreTuffEvent {

    public enum Action {
        GRANTED,
        DENIED,
        REVOKED
    }

    private final String addonId;
    private final Capability capability;
    private final TrustLevel trustLevel;
    private final Action action;
    private final String reason;

    public CapabilityEvent(String addonId, Capability capability, TrustLevel trustLevel, Action action, String reason) {
        this.addonId = addonId;
        this.capability = capability;
        this.trustLevel = trustLevel;
        this.action = action;
        this.reason = reason;
    }

    public String addonId() { return addonId; }
    public Capability capability() { return capability; }
    public TrustLevel trustLevel() { return trustLevel; }
    public Action action() { return action; }
    public String reason() { return reason; }
}
