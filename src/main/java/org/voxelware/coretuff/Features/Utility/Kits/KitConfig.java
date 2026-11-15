package org.voxelware.coretuff.Features.Utility.Kits;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.Utility.VersionedConfigLoader;

import java.util.Set;

public class KitConfig {

    private final VersionedConfigLoader loader;

    public KitConfig(CoreTuff plugin) {
        loader = new VersionedConfigLoader(plugin, "kitsconfig.yml", "Core/kitsconfig.yml");
    }

    public void reload() {
        loader.reload();
    }

    public FileConfiguration get() {
        return loader.getConfig();
    }

    public int kitTeleportDelay() {
        return get().getInt("kit-teleport-delay-seconds", 0);
    }

    public int defaultCooldown() {
        return get().getInt("default-cooldown-seconds", 0);
    }

    public Set<String> getKitNames() {
        ConfigurationSection section = get().getConfigurationSection("kits");
        return section == null ? Set.of() : section.getKeys(false);
    }

    public ConfigurationSection getKitSection(String name) {
        return get().getConfigurationSection("kits." + name);
    }
}
