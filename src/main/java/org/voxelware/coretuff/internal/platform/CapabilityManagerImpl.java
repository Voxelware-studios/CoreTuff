package org.voxelware.coretuff.internal.platform;

import org.voxelware.coretuff.api.addon.AddonManifest;
import org.voxelware.coretuff.api.addon.Capability;
import org.voxelware.coretuff.api.addon.TrustLevel;
import org.voxelware.coretuff.api.platform.CapabilityManager;

import java.util.*;

public class CapabilityManagerImpl implements CapabilityManager {

    private static final Map<Capability, CapabilityPolicy> POLICIES = new EnumMap<>(Capability.class);

    static {
        POLICIES.put(Capability.ECONOMY, new CapabilityPolicy(Capability.ECONOMY, TrustLevel.TRUST_INTERNAL, false));
        POLICIES.put(Capability.TRANSACTIONS, new CapabilityPolicy(Capability.TRANSACTIONS, TrustLevel.TRUST_INTERNAL, false));
        POLICIES.put(Capability.HOMES, new CapabilityPolicy(Capability.HOMES, TrustLevel.TRUST_PARTNER, false));
        POLICIES.put(Capability.WARPS, new CapabilityPolicy(Capability.WARPS, TrustLevel.TRUST_PARTNER, false));
        POLICIES.put(Capability.PLAYER_DATA, new CapabilityPolicy(Capability.PLAYER_DATA, TrustLevel.TRUST_INTERNAL, true));
        POLICIES.put(Capability.PLACEHOLDER_API, new CapabilityPolicy(Capability.PLACEHOLDER_API, TrustLevel.TRUST_PUBLIC, false));
        POLICIES.put(Capability.WEB, new CapabilityPolicy(Capability.WEB, TrustLevel.TRUST_INTERNAL, true));
        POLICIES.put(Capability.SCHEDULER, new CapabilityPolicy(Capability.SCHEDULER, TrustLevel.TRUST_PUBLIC, false));
        POLICIES.put(Capability.CONFIG, new CapabilityPolicy(Capability.CONFIG, TrustLevel.TRUST_PARTNER, false));
        POLICIES.put(Capability.DISCORD, new CapabilityPolicy(Capability.DISCORD, TrustLevel.TRUST_INTERNAL, true));
        POLICIES.put(Capability.TELEPORTATION, new CapabilityPolicy(Capability.TELEPORTATION, TrustLevel.TRUST_PARTNER, false));
        POLICIES.put(Capability.EXECUTION, new CapabilityPolicy(Capability.EXECUTION, TrustLevel.TRUST_INTERNAL, true));
        POLICIES.put(Capability.LAND, new CapabilityPolicy(Capability.LAND, TrustLevel.TRUST_PARTNER, false));
        POLICIES.put(Capability.MODERATION, new CapabilityPolicy(Capability.MODERATION, TrustLevel.TRUST_INTERNAL, true));
        POLICIES.put(Capability.KITS, new CapabilityPolicy(Capability.KITS, TrustLevel.TRUST_PARTNER, false));
        POLICIES.put(Capability.SECURITY, new CapabilityPolicy(Capability.SECURITY, TrustLevel.TRUST_INTERNAL, true));
        POLICIES.put(Capability.UTILITY, new CapabilityPolicy(Capability.UTILITY, TrustLevel.TRUST_PARTNER, false));
        POLICIES.put(Capability.ANALYTICS, new CapabilityPolicy(Capability.ANALYTICS, TrustLevel.TRUST_INTERNAL, false));
        POLICIES.put(Capability.PLAYER, new CapabilityPolicy(Capability.PLAYER, TrustLevel.TRUST_PARTNER, false));
    }

    @Override
    public Set<Capability> resolve(AddonManifest manifest) {
        return resolve(manifest.requestedCapabilities(), manifest.trustLevel());
    }

    @Override
    public Set<Capability> resolve(Set<Capability> requested, TrustLevel trustLevel) {
        Set<Capability> granted = EnumSet.noneOf(Capability.class);
        for (Capability cap : requested) {
            if (isCapabilityAllowed(cap, trustLevel)) {
                granted.add(cap);
            }
        }
        return Collections.unmodifiableSet(granted);
    }

    @Override
    public boolean isCapabilityAllowed(Capability capability, TrustLevel trustLevel) {
        CapabilityPolicy policy = POLICIES.get(capability);
        if (policy == null) return false;
        return trustLevel.ordinal() <= policy.minimumTrust().ordinal();
    }

    @Override
    public CapabilityPolicy policyFor(Capability capability) {
        return POLICIES.get(capability);
    }
}
