package org.voxelware.coretuff.features.security;

import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.database.config.ConfigurationService;
import org.voxelware.coretuff.database.config.DatabaseConfigProvider;

public final class SecurityConfig {

    private static final String PLUGIN_HIDE_KEY = "plugin-hide";
    private static final String HIDDEN_CMD_KEY = "messages.hidden-command";

    private final DatabaseConfigProvider db;
    private ConfigurationService configService;

    public SecurityConfig() {
        this.db = new DatabaseConfigProvider("security");
    }

    public void setConfigurationService(ConfigurationService configService) {
        this.configService = configService;
        db.setConfigurationService(configService);
    }

    public void reload() {
        db.reload();
    }

    public void save() {
    }

    public FileConfiguration get() {
        return db.getConfig();
    }

    public boolean pluginHideEnabled() {
        return db.getBoolean(PLUGIN_HIDE_KEY, false);
    }

    public void setPluginHide(boolean value) {
        if (configService != null) {
            configService.set("security", PLUGIN_HIDE_KEY, String.valueOf(value));
        }
    }

    public String hiddenCommandMessage() {
        return db.getString(HIDDEN_CMD_KEY, "&cUnknown command.");
    }
}
