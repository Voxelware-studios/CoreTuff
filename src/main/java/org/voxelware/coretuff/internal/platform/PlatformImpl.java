package org.voxelware.coretuff.internal.platform;

import org.voxelware.coretuff.api.addon.*;
import org.voxelware.coretuff.api.config.AddonConfig;
import org.voxelware.coretuff.api.diagnostics.DiagnosticProvider;
import org.voxelware.coretuff.api.economy.EconomyApi;
import org.voxelware.coretuff.api.economy.InterestApi;
import org.voxelware.coretuff.api.economy.TransactionApi;
import org.voxelware.coretuff.api.placeholder.PlaceholderRegistrar;
import org.voxelware.coretuff.api.platform.*;
import org.voxelware.coretuff.api.scheduler.Scheduler;
import org.voxelware.coretuff.internal.addon.AddonLoaderImpl;
import org.voxelware.coretuff.internal.addon.LifecycleManagerImpl;
import org.voxelware.coretuff.internal.config.AddonConfigManager;
import org.voxelware.coretuff.internal.diagnostics.DiagnosticProviderRegistry;
import org.voxelware.coretuff.internal.placeholder.PlaceholderRegistrarImpl;
import org.voxelware.coretuff.internal.scheduler.SchedulerImpl;

import org.bukkit.plugin.Plugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class PlatformImpl implements Platform {

    private static final Logger LOG = Logger.getLogger(PlatformImpl.class.getName());

    private final String name;
    private final String version;
    private final boolean folia;

    private final ServiceRegistryImpl serviceRegistry;
    private final VersionCompatibilityManagerImpl versionCompatibility;
    private final DependencyResolverImpl dependencyResolver;
    private final SignatureVerifierImpl signatureVerifier;
    private final CapabilityManagerImpl capabilityManager;
    private final PlatformSecurityManagerImpl securityManager;
    private final SchedulerImpl scheduler;
    private final PlaceholderRegistrarImpl placeholderRegistrar;
    private final AddonConfigManager configManager;
    private final DiagnosticProviderRegistry diagnosticRegistry;

    private final AddonLoaderImpl addonLoader;
    private final LifecycleManagerImpl lifecycleManager;

    private final Map<String, AddonLifecycle> running = new LinkedHashMap<>();
    private final Map<String, AddonContext> contexts = new LinkedHashMap<>();

    public PlatformImpl(
            String name,
            String version,
            boolean folia,
            File addonsDir,
            File configsDir,
            File logsDir,
            File cacheDir,
            ClassLoader platformClassLoader,
            Plugin plugin,
            EconomyApi economyApi,
            TransactionApi transactionApi,
            InterestApi interestApi) {
        this.name = name;
        this.version = version;
        this.folia = folia;

        this.serviceRegistry = new ServiceRegistryImpl();
        this.versionCompatibility = new VersionCompatibilityManagerImpl();
        this.dependencyResolver = new DependencyResolverImpl();
        this.signatureVerifier = new SignatureVerifierImpl();
        this.capabilityManager = new CapabilityManagerImpl();
        this.securityManager = new PlatformSecurityManagerImpl();
        this.scheduler = new SchedulerImpl(plugin, folia);
        this.placeholderRegistrar = new PlaceholderRegistrarImpl();
        this.configManager = new AddonConfigManager(plugin, configsDir);
        this.diagnosticRegistry = new DiagnosticProviderRegistry();

        configsDir.mkdirs();
        logsDir.mkdirs();
        cacheDir.mkdirs();

        this.addonLoader = new AddonLoaderImpl(platformClassLoader);
        this.lifecycleManager = new LifecycleManagerImpl(
                addonsDir, configsDir, logsDir, cacheDir,
                folia, version, economyApi, transactionApi, interestApi,
                securityManager, scheduler, this, placeholderRegistrar, configManager);

        serviceRegistry.register(Scheduler.class, scheduler, ServiceRegistry.ServicePriority.NORMAL);
        serviceRegistry.register(PlaceholderRegistrar.class, placeholderRegistrar, ServiceRegistry.ServicePriority.NORMAL);
        serviceRegistry.register(Platform.class, this, ServiceRegistry.ServicePriority.NORMAL);
    }

    @Override
    public String name() { return name; }

    @Override
    public String version() { return version; }

    @Override
    public boolean isFolia() { return folia; }

    @Override
    public ServiceRegistry serviceRegistry() { return serviceRegistry; }

    @Override
    public VersionCompatibilityManager versionCompatibility() { return versionCompatibility; }

    @Override
    public DependencyResolver dependencyResolver() { return dependencyResolver; }

    @Override
    public SignatureVerifier signatureVerifier() { return signatureVerifier; }

    @Override
    public CapabilityManager capabilityManager() { return capabilityManager; }

    @Override
    public PlatformSecurityManager securityManager() { return securityManager; }

    @Override
    public Logger platformLogger() { return LOG; }

    public Scheduler scheduler() { return scheduler; }

    public PlaceholderRegistrar placeholderRegistrar() { return placeholderRegistrar; }

    public AddonConfigManager configManager() { return configManager; }

    public DiagnosticProviderRegistry diagnosticRegistry() { return diagnosticRegistry; }

    public void registerDiagnosticProvider(DiagnosticProvider provider) {
        diagnosticRegistry.register(provider);
    }

    @Override
    public AddonLoadResult loadAddon(Path jarPath) {
        AddonLoadResult loadResult = addonLoader.load(jarPath);
        if (!loadResult.success()) return loadResult;

        AddonManifest manifest = loadResult.manifest();
        if (manifest == null) {
            return AddonLoadResult.failure(null, List.of("Unknown manifest after load"));
        }

        AddonLoaderImpl.LoadedAddon loaded = addonLoader.get(manifest.id());
        if (loaded == null) {
            return AddonLoadResult.failure(manifest, List.of("Addon not found after load"));
        }

        VersionCompatibilityManager.CompatibilityResult compat =
                versionCompatibility.check(manifest, version);
        if (!compat.compatible()) {
            addonLoader.unload(manifest.id());
            return AddonLoadResult.failure(manifest, List.of(compat.reason()));
        }

        if (manifest.isSigned()) {
            var sigResult = signatureVerifier.verify(manifest, jarPath);
            if (!sigResult.valid()) {
                String msg = "Signature verification failed for '" + manifest.id() + "': " + sigResult.detail();
                LOG.warning(msg);
                if (manifest.trustLevel() == TrustLevel.TRUST_INTERNAL) {
                    addonLoader.unload(manifest.id());
                    return AddonLoadResult.failure(manifest, List.of(msg));
                }
            }
        }

        Set<Capability> granted = capabilityManager.resolve(manifest);

        LifecycleManagerImpl.InjectResult inject = lifecycleManager.injectAndLoad(loaded, granted, manifest.trustLevel());
        if (!inject.success()) {
            addonLoader.unload(manifest.id());
            return AddonLoadResult.failure(manifest, List.of(inject.error()));
        }

        contexts.put(manifest.id(), inject.context());

        List<String> warnings = new ArrayList<>();
        if (!manifest.isSigned()) {
            warnings.add("Unsigned addon — trust level: " + manifest.trustLevel());
        }
        for (Capability requested : manifest.requestedCapabilities()) {
            if (!granted.contains(requested)) {
                warnings.add("Capability '" + requested + "' was requested but not granted (trust level: " + manifest.trustLevel() + ")");
            }
        }

        return warnings.isEmpty()
                ? AddonLoadResult.success(manifest)
                : AddonLoadResult.success(manifest, warnings);
    }

    @Override
    public List<AddonLoadResult> loadAllAddons(Path addonsDirectory) {
        List<AddonLoadResult> results = new ArrayList<>();
        List<AddonManifest> manifests = new ArrayList<>();
        Map<String, Path> jarPaths = new LinkedHashMap<>();

        if (!Files.isDirectory(addonsDirectory)) {
            try {
                Files.createDirectories(addonsDirectory);
            } catch (IOException e) {
                LOG.warning("Could not create addons directory: " + e.getMessage());
            }
            return results;
        }

        try (Stream<Path> files = Files.list(addonsDirectory)) {
            List<Path> jars = files.filter(p -> p.toString().endsWith(".jar")).toList();
            for (Path jar : jars) {
                AddonManifest m = org.voxelware.coretuff.internal.addon.ManifestParser.parse(jar);
                if (m != null) {
                    manifests.add(m);
                    jarPaths.put(m.id(), jar);
                } else {
                    results.add(AddonLoadResult.failure(null,
                            List.of("Missing or invalid addon.yml in " + jar.getFileName())));
                }
            }
        } catch (IOException e) {
            results.add(AddonLoadResult.failure(null,
                    List.of("Failed to list addons directory: " + e.getMessage())));
            return results;
        }

        DependencyResolver.ResolutionResult resolution = dependencyResolver.resolve(manifests);
        if (!resolution.success()) {
            LOG.severe("Dependency resolution failed: " + String.join("; ", resolution.errors()));
            results.add(AddonLoadResult.failure(null, resolution.errors()));
            return results;
        }

        for (AddonManifest manifest : resolution.ordered()) {
            Path jarPath = jarPaths.get(manifest.id());
            if (jarPath != null) {
                AddonLoadResult r = loadAddon(jarPath);
                results.add(r);
                if (r.success()) {
                    AddonLoaderImpl.LoadedAddon loaded = addonLoader.get(manifest.id());
                    if (loaded != null) {
                        AddonContext ctx = contexts.get(manifest.id());
                        if (ctx != null) {
                            lifecycleManager.enable(loaded, ctx);
                            running.put(manifest.id(), loaded.instance());
                        }
                    }
                }
            }
        }

        return results;
    }

    @Override
    public void enableAddon(String addonId) {
        AddonLoaderImpl.LoadedAddon loaded = addonLoader.get(addonId);
        if (loaded != null && !running.containsKey(addonId)) {
            AddonContext ctx = contexts.get(addonId);
            if (ctx != null) {
                lifecycleManager.enable(loaded, ctx);
                running.put(addonId, loaded.instance());
            }
        }
    }

    @Override
    public void disableAddon(String addonId) {
        AddonLoaderImpl.LoadedAddon loaded = addonLoader.get(addonId);
        if (loaded != null) {
            lifecycleManager.disable(loaded);
            running.remove(addonId);
        }
    }

    @Override
    public AddonLifecycle getAddon(String addonId) {
        return running.get(addonId);
    }

    @Override
    public void registerAddon(String id, AddonLifecycle addon, AddonManifest manifest) {
        running.put(id, addon);
    }

    @Override
    public Set<Capability> grantedCapabilities(String addonId) {
        return securityManager.getGrantedCapabilities(addonId);
    }

    public List<String> getAllAddonIds() {
        return new ArrayList<>(running.keySet());
    }

    public void shutdown() {
        for (AddonLoaderImpl.LoadedAddon loaded : addonLoader.allLoaded().values()) {
            if (running.containsKey(loaded.manifest().id())) {
                lifecycleManager.disable(loaded);
            }
            addonLoader.unload(loaded.manifest().id());
        }
        running.clear();
        contexts.clear();
    }
}
