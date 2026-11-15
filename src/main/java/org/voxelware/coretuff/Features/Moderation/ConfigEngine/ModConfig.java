package org.voxelware.coretuff.Features.Moderation.ConfigEngine;

import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.database.config.ConfigurationService;
import org.voxelware.coretuff.database.config.DatabaseConfigProvider;

public class ModConfig {
    private final DatabaseConfigProvider db;

    public ModConfig() {
        this.db = new DatabaseConfigProvider("moderation");
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

    public int jailTeleportDelay() {
        return db.getInt("moderation.jail.teleport-delay-seconds", 3);
    }

    public boolean jailReleaseOnLogout() {
        return db.getBoolean("moderation.jail.release-on-logout", true);
    }

    public String jailMessage(String path, String fallback) {
        return db.getString("moderation.jail.messages." + path, fallback);
    }
}
