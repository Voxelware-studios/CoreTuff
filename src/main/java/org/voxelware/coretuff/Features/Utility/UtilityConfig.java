package org.voxelware.coretuff.Features.Utility;

import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.Utility.VersionedConfigLoader;

public class UtilityConfig {
    private final VersionedConfigLoader loader;

    public UtilityConfig(CoreTuff plugin) {
        loader = new VersionedConfigLoader(plugin, "utilityconfig.yml", "Core/utilityconfig.yml");
    }

    public void reload() {
        loader.reload();
    }

    public FileConfiguration get() {
        return loader.getConfig();
    }
}
