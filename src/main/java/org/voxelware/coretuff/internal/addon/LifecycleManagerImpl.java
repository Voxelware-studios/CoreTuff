package org.voxelware.coretuff.internal.addon;

import org.voxelware.coretuff.api.addon.AddonContext;
import org.voxelware.coretuff.api.addon.AddonLifecycle;
import org.voxelware.coretuff.api.addon.AddonManifest;
import org.voxelware.coretuff.api.addon.Capability;
import org.voxelware.coretuff.api.addon.TrustLevel;
import org.voxelware.coretuff.api.economy.EconomyApi;
import org.voxelware.coretuff.api.economy.InterestApi;
import org.voxelware.coretuff.api.economy.TransactionApi;
import org.voxelware.coretuff.api.placeholder.PlaceholderRegistrar;
import org.voxelware.coretuff.api.platform.Platform;
import org.voxelware.coretuff.api.scheduler.Scheduler;
import org.voxelware.coretuff.internal.config.AddonConfigManager;
import org.voxelware.coretuff.internal.logging.AddonLoggerFactory;
import org.voxelware.coretuff.internal.platform.PlatformSecurityManagerImpl;

import java.io.File;
import java.util.Set;
import java.util.logging.Logger;

public class LifecycleManagerImpl {

    private static final Logger LOG = Logger.getLogger(LifecycleManagerImpl.class.getName());

    private final File addonsDir;
    private final File configsDir;
    private final File logsDir;
    private final File cacheDir;
    private final boolean folia;
    private final String coreVersion;
    private final EconomyApi economyApi;
    private final TransactionApi transactionApi;
    private final InterestApi interestApi;
    private final PlatformSecurityManagerImpl securityManager;
    private final Scheduler scheduler;
    private final Platform platform;
    private final PlaceholderRegistrar placeholderRegistrar;
    private final AddonConfigManager configManager;

    public LifecycleManagerImpl(
            File addonsDir,
            File configsDir,
            File logsDir,
            File cacheDir,
            boolean folia,
            String coreVersion,
            EconomyApi economyApi,
            TransactionApi transactionApi,
            InterestApi interestApi,
            PlatformSecurityManagerImpl securityManager,
            Scheduler scheduler,
            Platform platform,
            PlaceholderRegistrar placeholderRegistrar,
            AddonConfigManager configManager) {
        this.addonsDir = addonsDir;
        this.configsDir = configsDir;
        this.logsDir = logsDir;
        this.cacheDir = cacheDir;
        this.folia = folia;
        this.coreVersion = coreVersion;
        this.economyApi = economyApi;
        this.transactionApi = transactionApi;
        this.interestApi = interestApi;
        this.securityManager = securityManager;
        this.scheduler = scheduler;
        this.platform = platform;
        this.placeholderRegistrar = placeholderRegistrar;
        this.configManager = configManager;
    }

    public InjectResult injectAndLoad(AddonLoaderImpl.LoadedAddon loadedAddon, Set<Capability> granted,
                                      TrustLevel trustLevel) {
        AddonManifest manifest = loadedAddon.manifest();
        Logger logger = AddonLoggerFactory.create(manifest.id(), manifest.name());

        File dataFolder = new File(addonsDir, manifest.id());
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File addonConfigsDir = new File(configsDir, manifest.id());
        File addonLogsDir = new File(logsDir, manifest.id());
        File addonCacheDir = new File(cacheDir, manifest.id());
        addonConfigsDir.mkdirs();
        addonLogsDir.mkdirs();
        addonCacheDir.mkdirs();

        AddonContext context = new AddonContextImpl(
                manifest, dataFolder, addonConfigsDir, addonLogsDir, addonCacheDir,
                new File(addonsDir, manifest.id() + ".jar"),
                logger, granted, trustLevel, folia, coreVersion,
                economyApi, transactionApi, interestApi,
                scheduler, platform, placeholderRegistrar, configManager);

        securityManager.registerAddonCapabilities(manifest.id(), granted, trustLevel);

        try {
            LOG.info("Loading addon: " + manifest.name() + " v" + manifest.version());
            loadedAddon.instance().onLoad(context);
            LOG.info("Loaded addon: " + manifest.name());
            return new InjectResult(true, context, null);
        } catch (Exception e) {
            LOG.severe("Failed to load addon '" + manifest.name() + "': " + e.getMessage());
            return new InjectResult(false, context, e.getMessage());
        }
    }

    public void enable(AddonLoaderImpl.LoadedAddon loadedAddon, AddonContext context) {
        try {
            LOG.info("Enabling addon: " + loadedAddon.manifest().name());
            loadedAddon.instance().onEnable(context);
            LOG.info("Enabled addon: " + loadedAddon.manifest().name());
        } catch (Exception e) {
            LOG.severe("Failed to enable addon '" + loadedAddon.manifest().name() + "': " + e.getMessage());
        }
    }

    public void disable(AddonLoaderImpl.LoadedAddon loadedAddon) {
        try {
            loadedAddon.instance().onDisable();
            securityManager.unregisterAddon(loadedAddon.manifest().id());
            LOG.info("Disabled addon: " + loadedAddon.manifest().name());
        } catch (Exception e) {
            LOG.severe("Failed to disable addon '" + loadedAddon.manifest().name() + "': " + e.getMessage());
        }
    }

    public record InjectResult(boolean success, AddonContext context, String error) {}
}
