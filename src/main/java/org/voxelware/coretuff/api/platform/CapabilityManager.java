package org.voxelware.coretuff.api.platform;

import org.voxelware.coretuff.api.addon.AddonManifest;
import org.voxelware.coretuff.api.addon.Capability;
import org.voxelware.coretuff.api.addon.TrustLevel;

import java.util.Set;

public interface CapabilityManager {

    Set<Capability> resolve(AddonManifest manifest);

    Set<Capability> resolve(Set<Capability> requested, TrustLevel trustLevel);

    boolean isCapabilityAllowed(Capability capability, TrustLevel trustLevel);

    CapabilityPolicy policyFor(Capability capability);

    record CapabilityPolicy(Capability capability, TrustLevel minimumTrust, boolean dangerous) {}
}
