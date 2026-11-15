package org.voxelware.coretuff.api.addon;

import java.util.*;

public final class AddonLoadResult {
    private final AddonManifest manifest;
    private final boolean success;
    private final List<String> errors;
    private final List<String> warnings;

    private AddonLoadResult(AddonManifest manifest, boolean success, List<String> errors, List<String> warnings) {
        this.manifest = manifest;
        this.success = success;
        this.errors = List.copyOf(errors);
        this.warnings = List.copyOf(warnings);
    }

    public static AddonLoadResult success(AddonManifest manifest) {
        return new AddonLoadResult(manifest, true, List.of(), List.of());
    }

    public static AddonLoadResult success(AddonManifest manifest, List<String> warnings) {
        return new AddonLoadResult(manifest, true, List.of(), warnings);
    }

    public static AddonLoadResult failure(AddonManifest manifest, List<String> errors) {
        return new AddonLoadResult(manifest, false, errors, List.of());
    }

    public AddonManifest manifest() { return manifest; }
    public boolean success() { return success; }
    public List<String> errors() { return errors; }
    public List<String> warnings() { return warnings; }
}
