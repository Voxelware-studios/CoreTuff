package org.voxelware.coretuff.Features.Utility;

import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.database.config.ConfigurationService;
import org.voxelware.coretuff.database.config.DatabaseConfigProvider;

public class UtilityConfig {
    private final DatabaseConfigProvider db;

    public UtilityConfig() {
        this.db = new DatabaseConfigProvider("utility");
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
