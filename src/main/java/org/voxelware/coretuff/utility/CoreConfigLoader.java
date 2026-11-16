package org.voxelware.coretuff.utility;

import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.database.config.ConfigurationService;
import org.voxelware.coretuff.database.config.DatabaseConfigProvider;

public class CoreConfigLoader {

    private final DatabaseConfigProvider db;

    public CoreConfigLoader() {
        this.db = new DatabaseConfigProvider("core");
    }

    public void setConfigurationService(ConfigurationService configService) {
        db.setConfigurationService(configService);
    }

    public FileConfiguration getConfig() {
        return db.getConfig();
    }

    public void reload() {
        db.reload();
    }

    public void save() {
    }

    public void saveConfig() {
    }

    public String getString(String path, String fallback) {
        return db.getString(path, fallback);
    }

    public String getString(String path) {
        return db.getString(path, null);
    }

    public boolean getBoolean(String path, boolean fallback) {
        return db.getBoolean(path, fallback);
    }

    public int getInt(String path, int fallback) {
        return db.getInt(path, fallback);
    }
}
