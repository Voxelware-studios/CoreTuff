package org.voxelware.coretuff.internal.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.voxelware.coretuff.api.config.AddonConfig;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AddonConfigManager {

    private final Plugin plugin;
    private final File configsDir;
    private final Map<String, CoreTuffAddonConfig> configs = new ConcurrentHashMap<>();

    public AddonConfigManager(Plugin plugin, File configsDir) {
        this.plugin = plugin;
        this.configsDir = configsDir;
        if (!configsDir.exists()) {
            configsDir.mkdirs();
        }
    }

    public AddonConfig create(String addonId, String fileName) {
        String key = addonId + ":" + fileName;
        return configs.computeIfAbsent(key, k -> {
            File file = new File(configsDir, addonId + "/" + fileName);
            return new CoreTuffAddonConfig(file);
        });
    }

    public AddonConfig get(String addonId, String fileName) {
        return configs.get(addonId + ":" + fileName);
    }

    public void reloadAll() {
        for (CoreTuffAddonConfig config : configs.values()) {
            config.reload();
        }
    }

    public void reload(String addonId) {
        for (Map.Entry<String, CoreTuffAddonConfig> entry : configs.entrySet()) {
            if (entry.getKey().startsWith(addonId + ":")) {
                entry.getValue().reload();
            }
        }
    }

    private static class CoreTuffAddonConfig implements AddonConfig {

        private final File file;
        private FileConfiguration config;

        CoreTuffAddonConfig(File file) {
            this.file = file;
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            this.config = YamlConfiguration.loadConfiguration(file);
            save();
        }

        @Override
        public String name() { return file.getName(); }

        @Override
        public String path() { return file.getAbsolutePath(); }

        @Override
        public synchronized void reload() {
            this.config = YamlConfiguration.loadConfiguration(file);
        }

        @Override
        public synchronized void save() {
            try {
                config.save(file);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save config: " + file, e);
            }
        }

        @Override
        public boolean contains(String path) { return config.contains(path); }

        @Override
        public String getString(String path) { return config.getString(path); }

        @Override
        public String getString(String path, String fallback) { return config.getString(path, fallback); }

        @Override
        public int getInt(String path) { return config.getInt(path); }

        @Override
        public int getInt(String path, int fallback) { return config.getInt(path, fallback); }

        @Override
        public double getDouble(String path) { return config.getDouble(path); }

        @Override
        public double getDouble(String path, double fallback) { return config.getDouble(path, fallback); }

        @Override
        public boolean getBoolean(String path) { return config.getBoolean(path); }

        @Override
        public boolean getBoolean(String path, boolean fallback) { return config.getBoolean(path, fallback); }

        @Override
        public List<String> getStringList(String path) { return config.getStringList(path); }

        @Override
        public Map<String, Object> getValues(boolean deep) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(config.getValues(deep)));
        }

        @Override
        public void set(String path, Object value) {
            config.set(path, value);
            save();
        }

        @Override
        public int configVersion() {
            return config.getInt("config-version", 1);
        }

        @Override
        public boolean migrate(int targetVersion, String targetSection) {
            int current = configVersion();
            if (current >= targetVersion) return false;
            if (config.contains(targetSection)) {
                Map<String, Object> values = config.getConfigurationSection(targetSection).getValues(false);
                config.set(targetSection, null);
                config.createSection(targetSection, values);
            }
            config.set("config-version", targetVersion);
            save();
            return true;
        }
    }
}
