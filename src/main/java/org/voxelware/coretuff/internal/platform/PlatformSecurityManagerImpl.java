package org.voxelware.coretuff.internal.platform;

import org.voxelware.coretuff.api.addon.Capability;
import org.voxelware.coretuff.api.addon.TrustLevel;
import org.voxelware.coretuff.api.platform.PlatformSecurityManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PlatformSecurityManagerImpl implements PlatformSecurityManager {

    private final Map<String, Set<Capability>> grantedCapabilities = new ConcurrentHashMap<>();
    private final Map<String, TrustLevel> trustLevels = new ConcurrentHashMap<>();

    public void registerAddonCapabilities(String addonId, Set<Capability> capabilities, TrustLevel trustLevel) {
        grantedCapabilities.put(addonId, capabilities);
        trustLevels.put(addonId, trustLevel);
    }

    public void unregisterAddon(String addonId) {
        grantedCapabilities.remove(addonId);
        trustLevels.remove(addonId);
    }

    @Override
    public void checkAccess(Object caller, Capability capability) {
        if (!hasCapability(caller, capability)) {
            denyAccess(caller, capability, "Addon lacks required capability: " + capability);
        }
    }

    @Override
    public boolean hasCapability(Object caller, Capability capability) {
        String id = resolveCallerId(caller);
        if (id == null) return false;
        Set<Capability> caps = grantedCapabilities.get(id);
        return caps != null && caps.contains(capability);
    }

    @Override
    public TrustLevel trustLevel(Object caller) {
        String id = resolveCallerId(caller);
        if (id == null) return TrustLevel.TRUST_PUBLIC;
        return trustLevels.getOrDefault(id, TrustLevel.TRUST_PUBLIC);
    }

    @Override
    public void denyAccess(Object caller, Capability capability, String reason) {
        String id = resolveCallerId(caller);
        throw new SecurityException("Capability denied [" + capability + "] for " + id + ": " + reason);
    }

    private String resolveCallerId(Object caller) {
        if (caller instanceof String) return (String) caller;
        if (caller instanceof ClassLoader cl) {
            return cl.toString();
        }
        return null;
    }

    public Set<Capability> getGrantedCapabilities(String addonId) {
        return grantedCapabilities.get(addonId);
    }
}
