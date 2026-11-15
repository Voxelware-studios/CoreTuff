package org.voxelware.coretuff.api.platform;

import org.voxelware.coretuff.api.addon.AddonManifest;

public interface VersionCompatibilityManager {

    boolean isCompatible(AddonManifest manifest, String coreVersion);

    CompatibilityResult check(AddonManifest manifest, String coreVersion);

    record CompatibilityResult(boolean compatible, String reason) {
        public static CompatibilityResult ok() {
            return new CompatibilityResult(true, null);
        }
        public static CompatibilityResult failed(String reason) {
            return new CompatibilityResult(false, reason);
        }
    }
}
