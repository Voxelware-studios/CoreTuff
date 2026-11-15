package org.voxelware.coretuff.api.platform;

import org.voxelware.coretuff.api.addon.Capability;
import org.voxelware.coretuff.api.addon.TrustLevel;

public interface PlatformSecurityManager {

    void checkAccess(Object caller, Capability capability);

    boolean hasCapability(Object caller, Capability capability);

    TrustLevel trustLevel(Object caller);

    void denyAccess(Object caller, Capability capability, String reason);
}
