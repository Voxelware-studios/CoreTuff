package org.voxelware.coretuff.Features.Utility.Kits;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.database.config.ConfigurationService;
import org.voxelware.coretuff.database.config.DatabaseConfigProvider;

import java.util.Set;

public class KitConfig {

    private final DatabaseConfigProvider db;

    public KitConfig() {
        this.db = new DatabaseConfigProvider("kits");
    }

    public void setConfigurationService(ConfigurationService configService) {
        db.setConfigurationService(configService);
    }

    public void reload() {
        db.reload();
    }

    public FileConfiguration get() {
        return db.getConfig();
    }

    public int kitTeleportDelay() {
        return db.getInt("kit-teleport-delay-seconds", 0);
    }

    public int defaultCooldown() {
        return db.getInt("default-cooldown-seconds", 0);
    }

    public Set<String> getKitNames() {
        ConfigurationSection section = db.getConfig().getConfigurationSection("kits");
        return section == null ? Set.of() : section.getKeys(false);
    }

    public ConfigurationSection getKitSection(String name) {
        return db.getConfig().getConfigurationSection("kits." + name);
    }
}
