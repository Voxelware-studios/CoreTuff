package org.voxelware.coretuff.api.event;

import org.voxelware.coretuff.api.addon.AddonManifest;

import java.time.Instant;

public class AddonLifecycleEvent extends CoreTuffEvent {

    public enum Phase {
        LOAD,
        ENABLE,
        DISABLE,
        UNLOAD
    }

    private final AddonManifest manifest;
    private final Phase phase;
    private final Instant timestamp;

    public AddonLifecycleEvent(AddonManifest manifest, Phase phase) {
        this.manifest = manifest;
        this.phase = phase;
        this.timestamp = Instant.now();
    }

    public AddonManifest manifest() { return manifest; }
    public Phase phase() { return phase; }
    public Instant timestamp() { return timestamp; }
}
