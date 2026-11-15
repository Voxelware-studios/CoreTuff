package org.voxelware.coretuff.internal.platform;

import org.voxelware.coretuff.api.addon.AddonManifest;
import org.voxelware.coretuff.api.platform.VersionCompatibilityManager;
import org.voxelware.coretuff.api.platform.VersionRange;

import java.util.logging.Logger;

public class VersionCompatibilityManagerImpl implements VersionCompatibilityManager {

    private static final Logger LOG = Logger.getLogger(VersionCompatibilityManagerImpl.class.getName());

    @Override
    public boolean isCompatible(AddonManifest manifest, String coreVersion) {
        return check(manifest, coreVersion).compatible();
    }

    @Override
    public CompatibilityResult check(AddonManifest manifest, String coreVersion) {
        String spec = manifest.coretuffVersion();
        if (spec == null || spec.isBlank()) {
            return CompatibilityResult.failed("Addon '" + manifest.id() + "' has no coretuffVersion specified");
        }
        try {
            VersionRange range = VersionRange.parse(spec);
            VersionRange.Version current = VersionRange.Version.parse(coreVersion);
            if (range.contains(current)) {
                LOG.fine("Addon '" + manifest.id() + "' version range " + spec
                        + " is compatible with CoreTuff " + coreVersion);
                return CompatibilityResult.ok();
            } else {
                String msg = "Addon '" + manifest.id() + "' requires CoreTuff " + spec
                        + " but current version is " + coreVersion;
                LOG.warning(msg);
                return CompatibilityResult.failed(msg);
            }
        } catch (IllegalArgumentException e) {
            String msg = "Invalid version range '" + spec + "' in addon '" + manifest.id() + "': " + e.getMessage();
            LOG.warning(msg);
            return CompatibilityResult.failed(msg);
        }
    }
}
