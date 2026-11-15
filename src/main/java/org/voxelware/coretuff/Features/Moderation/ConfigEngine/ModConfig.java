package org.voxelware.coretuff.Features.Moderation.ConfigEngine;

import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.Utility.VersionedConfigLoader;

public class ModConfig {
    private final VersionedConfigLoader loader;

    public ModConfig(CoreTuff plugin) {
        loader = new VersionedConfigLoader(plugin, "modconfig.yml", "Core/modconfig.yml");
    }

    public void reload() {
        loader.reload();
    }

    public FileConfiguration get() {
        return loader.getConfig();
    }

    public int jailTeleportDelay() {
        return get().getInt("moderation.jail.teleport-delay-seconds", 3);
    }

    public boolean jailReleaseOnLogout() {
        return get().getBoolean("moderation.jail.release-on-logout", true);
    }

    public String jailMessage(String path, String fallback) {
        return get().getString("moderation.jail.messages." + path, fallback);
    }
}
