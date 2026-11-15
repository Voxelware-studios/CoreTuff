package org.voxelware.coretuff.database.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.voxelware.coretuff.CoreTuff;

public final class DatabaseConfigProvider {

    private ConfigurationService configService;
    private final String module;
    private YamlConfiguration cachedConfig;

    public DatabaseConfigProvider(String module) {
        this.module = module;
        this.cachedConfig = new YamlConfiguration();
    }

    public void setConfigurationService(ConfigurationService configService) {
        this.configService = configService;
        reload();
    }

    public void reload() {
        if (configService == null) return;
        this.cachedConfig = new YamlConfiguration();
        var values = configService.getModule(module);
        for (var entry : values.entrySet()) {
            String raw = entry.getValue();
            if (raw == null) continue;
            cachedConfig.set(entry.getKey(), parseValue(raw));
        }
    }

    public YamlConfiguration getConfig() {
        return cachedConfig;
    }

    public boolean hasConfigService() {
        return configService != null;
    }

    public String getString(String key, String defaultValue) {
        if (configService != null) {
            return configService.get(module, key, defaultValue);
        }
        return cachedConfig.contains(key) ? cachedConfig.getString(key, defaultValue) : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        if (configService != null) {
            return configService.getInt(module, key, defaultValue);
        }
        return cachedConfig.getInt(key, defaultValue);
    }

    public double getDouble(String key, double defaultValue) {
        if (configService != null) {
            return configService.getDouble(module, key, defaultValue);
        }
        return cachedConfig.getDouble(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (configService != null) {
            return configService.getBoolean(module, key, defaultValue);
        }
        return cachedConfig.getBoolean(key, defaultValue);
    }

    public boolean set(String key, String value) {
        if (configService != null) {
            return configService.set(module, key, value);
        }
        cachedConfig.set(key, value);
        return true;
    }

    private static Object parseValue(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        if (raw.equalsIgnoreCase("true")) return true;
        if (raw.equalsIgnoreCase("false")) return false;
        try { return Integer.parseInt(raw); } catch (NumberFormatException e1) {
            try { return Double.parseDouble(raw); } catch (NumberFormatException e2) {
                if ((raw.startsWith("[") && raw.endsWith("]")) || (raw.startsWith("{") && raw.endsWith("}"))) {
                    try {
                        var el = com.google.gson.JsonParser.parseString(raw);
                        if (el.isJsonArray()) {
                            java.util.List<Object> list = new java.util.ArrayList<>();
                            for (var elem : el.getAsJsonArray()) {
                                if (elem.isJsonPrimitive()) {
                                    var p = elem.getAsJsonPrimitive();
                                    if (p.isBoolean()) list.add(p.getAsBoolean());
                                    else if (p.isNumber()) list.add(p.getAsNumber());
                                    else list.add(p.getAsString());
                                } else {
                                    list.add(elem.toString());
                                }
                            }
                            return list;
                        }
                    } catch (Exception ignored) {}
                }
                return raw;
            }
        }
    }
}
