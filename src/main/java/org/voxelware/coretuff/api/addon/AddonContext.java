package org.voxelware.coretuff.api.addon;

import org.bukkit.entity.Player;
import org.voxelware.coretuff.api.config.AddonConfig;
import org.voxelware.coretuff.api.economy.EconomyApi;
import org.voxelware.coretuff.api.economy.InterestApi;
import org.voxelware.coretuff.api.economy.TransactionApi;
import org.voxelware.coretuff.api.placeholder.PlaceholderRegistrar;
import org.voxelware.coretuff.api.platform.Platform;
import org.voxelware.coretuff.api.scheduler.Scheduler;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public interface AddonContext {

    AddonManifest manifest();

    String addonName();

    String addonVersion();

    String coreVersion();

    boolean isFolia();

    File addonDataFolder();

    File addonJar();

    File configsDir();

    File logsDir();

    File cacheDir();

    Logger logger();

    Set<Capability> grantedCapabilities();

    TrustLevel trustLevel();

    Scheduler scheduler();

    Platform platform();

    Map<String, Object> configSnapshot(String configName);

    AddonConfig getConfiguration(String fileName);

    void sendMessage(Player player, String message, Map<String, String> placeholders);

    EconomyApi getEconomyApi();

    TransactionApi getTransactionApi();

    InterestApi getInterestApi();

    PlaceholderRegistrar getPlaceholderApi();
}
