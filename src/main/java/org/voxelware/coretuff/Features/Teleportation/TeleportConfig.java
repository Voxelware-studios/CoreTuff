package org.voxelware.coretuff.Features.Teleportation;

import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.database.config.ConfigurationService;
import org.voxelware.coretuff.database.config.DatabaseConfigProvider;

public class TeleportConfig {

    private final DatabaseConfigProvider db;

    public TeleportConfig() {
        this.db = new DatabaseConfigProvider("teleport");
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
}
