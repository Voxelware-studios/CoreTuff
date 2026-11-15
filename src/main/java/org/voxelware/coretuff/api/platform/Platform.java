package org.voxelware.coretuff.api.platform;

import org.voxelware.coretuff.api.addon.AddonManifest;
import org.voxelware.coretuff.api.addon.AddonLifecycle;
import org.voxelware.coretuff.api.addon.AddonLoadResult;
import org.voxelware.coretuff.api.addon.Capability;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public interface Platform {

    String name();

    String version();

    boolean isFolia();

    ServiceRegistry serviceRegistry();

    VersionCompatibilityManager versionCompatibility();

    DependencyResolver dependencyResolver();

    SignatureVerifier signatureVerifier();

    CapabilityManager capabilityManager();

    PlatformSecurityManager securityManager();

    Logger platformLogger();

    AddonLoadResult loadAddon(Path jarPath);

    List<AddonLoadResult> loadAllAddons(Path addonsDirectory);

    void enableAddon(String addonId);

    void disableAddon(String addonId);

    AddonLifecycle getAddon(String addonId);

    void registerAddon(String id, AddonLifecycle addon, AddonManifest manifest);

    Set<Capability> grantedCapabilities(String addonId);
}
