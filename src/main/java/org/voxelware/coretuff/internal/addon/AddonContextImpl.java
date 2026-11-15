package org.voxelware.coretuff.internal.addon;

import org.bukkit.entity.Player;
import org.voxelware.coretuff.api.addon.AddonContext;
import org.voxelware.coretuff.api.addon.AddonManifest;
import org.voxelware.coretuff.api.addon.Capability;
import org.voxelware.coretuff.api.addon.TrustLevel;
import org.voxelware.coretuff.api.config.AddonConfig;
import org.voxelware.coretuff.api.economy.EconomyApi;
import org.voxelware.coretuff.api.economy.InterestApi;
import org.voxelware.coretuff.api.economy.TransactionApi;
import org.voxelware.coretuff.api.placeholder.PlaceholderRegistrar;
import org.voxelware.coretuff.api.platform.Platform;
import org.voxelware.coretuff.api.scheduler.Scheduler;
import org.voxelware.coretuff.internal.config.AddonConfigManager;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class AddonContextImpl implements AddonContext {

    private final AddonManifest manifest;
    private final File dataFolder;
    private final File configsFolder;
    private final File logsFolder;
    private final File cacheFolder;
    private final File jarFile;
    private final Logger logger;
    private final Set<Capability> grantedCapabilities;
    private final TrustLevel trustLevel;
    private final boolean folia;
    private final String coreVersion;
    private final EconomyApi economyApi;
    private final TransactionApi transactionApi;
    private final InterestApi interestApi;
    private final Scheduler scheduler;
    private final Platform platform;
    private final PlaceholderRegistrar placeholderRegistrar;
    private final AddonConfigManager configManager;

    public AddonContextImpl(
            AddonManifest manifest,
            File dataFolder,
            File configsFolder,
            File logsFolder,
            File cacheFolder,
            File jarFile,
            Logger logger,
            Set<Capability> grantedCapabilities,
            TrustLevel trustLevel,
            boolean folia,
            String coreVersion,
            EconomyApi economyApi,
            TransactionApi transactionApi,
            InterestApi interestApi,
            Scheduler scheduler,
            Platform platform,
            PlaceholderRegistrar placeholderRegistrar,
            AddonConfigManager configManager) {
        this.manifest = manifest;
        this.dataFolder = dataFolder;
        this.configsFolder = configsFolder;
        this.logsFolder = logsFolder;
        this.cacheFolder = cacheFolder;
        this.jarFile = jarFile;
        this.logger = logger;
        this.grantedCapabilities = grantedCapabilities;
        this.trustLevel = trustLevel;
        this.folia = folia;
        this.coreVersion = coreVersion;
        this.economyApi = economyApi;
        this.transactionApi = transactionApi;
        this.interestApi = interestApi;
        this.scheduler = scheduler;
        this.platform = platform;
        this.placeholderRegistrar = placeholderRegistrar;
        this.configManager = configManager;
    }

    @Override
    public AddonManifest manifest() { return manifest; }

    @Override
    public String addonName() { return manifest.name(); }

    @Override
    public String addonVersion() { return manifest.version(); }

    @Override
    public String coreVersion() { return coreVersion; }

    @Override
    public boolean isFolia() { return folia; }

    @Override
    public File addonDataFolder() { return dataFolder; }

    @Override
    public File addonJar() { return jarFile; }

    @Override
    public File configsDir() { return configsFolder; }

    @Override
    public File logsDir() { return logsFolder; }

    @Override
    public File cacheDir() { return cacheFolder; }

    @Override
    public Logger logger() { return logger; }

    @Override
    public Set<Capability> grantedCapabilities() { return grantedCapabilities; }

    @Override
    public TrustLevel trustLevel() { return trustLevel; }

    @Override
    public Scheduler scheduler() { return scheduler; }

    @Override
    public Platform platform() { return platform; }

    @Override
    public Map<String, Object> configSnapshot(String configName) { return Map.of(); }

    @Override
    public AddonConfig getConfiguration(String fileName) {
        return configManager.create(manifest.id(), fileName);
    }

    @Override
    public void sendMessage(Player player, String message, Map<String, String> placeholders) {}

    @Override
    public EconomyApi getEconomyApi() { return economyApi; }

    @Override
    public TransactionApi getTransactionApi() { return transactionApi; }

    @Override
    public InterestApi getInterestApi() { return interestApi; }

    @Override
    public PlaceholderRegistrar getPlaceholderApi() { return placeholderRegistrar; }
}
