package org.voxelware.coretuff.Features.Security;

import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.Utility.VersionedConfigLoader;

public final class SecurityConfig {

    private static final String PLUGIN_HIDE_KEY = "plugin-hide";
    private static final String HIDDEN_CMD_KEY = "messages.hidden-command";

    private final VersionedConfigLoader loader;

    public SecurityConfig(CoreTuff plugin) {
        loader = new VersionedConfigLoader(plugin, "securityconfig.yml", "Core/securityconfig.yml");
    }

    public void reload() {
        loader.reload();
    }

    public void save() {
        loader.save();
    }

    public FileConfiguration get() {
        return loader.getConfig();
    }

    public boolean pluginHideEnabled() {
        return get().getBoolean(PLUGIN_HIDE_KEY, false);
    }

    public void setPluginHide(boolean value) {
        get().set(PLUGIN_HIDE_KEY, value);
        save();
    }

    public String hiddenCommandMessage() {
        return get().getString(HIDDEN_CMD_KEY, "&cUnknown command.");
    }
}
