package org.voxelware.coretuff.dashboard.settings;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.database.config.ConfigurationService;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingDefinition;
import org.voxelware.coretuff.dashboard.settings.SettingsRegistry.SettingType;

import java.util.List;

public final class SettingsUtil {

    private static final Gson GSON = new Gson();
    private final CoreTuff plugin;
    private final ConfigurationService configService;

    public SettingsUtil(CoreTuff plugin, ConfigurationService configService) {
        this.plugin = plugin;
        this.configService = configService;
    }

    public Object toConfigValue(Object apiValue, SettingDefinition def) {
        if (apiValue == null) return def.defaultValue();
        return switch (def.type()) {
            case SettingType.StringType __ -> String.valueOf(apiValue);
            case SettingType.IntegerType __ -> {
                if (apiValue instanceof Number n) yield n.intValue();
                try { yield Integer.parseInt(String.valueOf(apiValue)); }
                catch (NumberFormatException e) { yield def.defaultValue(); }
            }
            case SettingType.DoubleType __ -> {
                if (apiValue instanceof Number n) yield n.doubleValue();
                try { yield Double.parseDouble(String.valueOf(apiValue)); }
                catch (NumberFormatException e) { yield def.defaultValue(); }
            }
            case SettingType.LongType __ -> {
                if (apiValue instanceof Number n) yield n.longValue();
                try { yield Long.parseLong(String.valueOf(apiValue)); }
                catch (NumberFormatException e) { yield def.defaultValue(); }
            }
            case SettingType.BooleanType __ -> {
                if (apiValue instanceof Boolean b) yield b;
                yield Boolean.parseBoolean(String.valueOf(apiValue));
            }
            case SettingType.EnumType __ -> String.valueOf(apiValue);
            case SettingType.ListType __ -> {
                if (apiValue instanceof List<?> l) yield l;
                yield def.defaultValue();
            }
            case SettingType.MapType __ -> {
                if (apiValue instanceof java.util.Map<?,?> m) yield m;
                yield def.defaultValue();
            }
            case SettingType.ColorType __ -> String.valueOf(apiValue);
            case SettingType.DurationType __ -> String.valueOf(apiValue);
            case SettingType.JsonType __ -> {
                if (apiValue instanceof String s) yield s;
                yield GSON.toJson(apiValue);
            }
            case SettingType.PasswordType __ -> String.valueOf(apiValue);
        };
    }

    public Object toApiValue(Object configValue, SettingDefinition def) {
        if (configValue == null) return def.defaultValue();
        return switch (def.type()) {
            case SettingType.StringType __ -> String.valueOf(configValue);
            case SettingType.IntegerType __ -> {
                if (configValue instanceof Number n) yield n.intValue();
                try { yield Integer.parseInt(String.valueOf(configValue)); }
                catch (NumberFormatException e) { yield def.defaultValue(); }
            }
            case SettingType.DoubleType __ -> {
                if (configValue instanceof Number n) yield n.doubleValue();
                try { yield Double.parseDouble(String.valueOf(configValue)); }
                catch (NumberFormatException e) { yield def.defaultValue(); }
            }
            case SettingType.LongType __ -> {
                if (configValue instanceof Number n) yield n.longValue();
                try { yield Long.parseLong(String.valueOf(configValue)); }
                catch (NumberFormatException e) { yield def.defaultValue(); }
            }
            case SettingType.BooleanType __ -> {
                if (configValue instanceof Boolean b) yield b;
                yield Boolean.parseBoolean(String.valueOf(configValue));
            }
            case SettingType.EnumType __ -> String.valueOf(configValue);
            case SettingType.ListType __ -> {
                if (configValue instanceof List<?> l) yield l;
                yield def.defaultValue();
            }
            case SettingType.MapType __ -> {
                if (configValue instanceof java.util.Map<?,?> m) yield m;
                yield def.defaultValue();
            }
            case SettingType.ColorType __ -> String.valueOf(configValue);
            case SettingType.DurationType __ -> String.valueOf(configValue);
            case SettingType.JsonType __ -> {
                if (configValue instanceof String s) yield s;
                yield GSON.toJson(configValue);
            }
            case SettingType.PasswordType __ -> String.valueOf(configValue);
        };
    }

    @SuppressWarnings("unchecked")
    public boolean validateValue(Object value, SettingDefinition def) {
        if (value == null) return false;
        boolean typeOk = switch (def.type()) {
            case SettingType.StringType __ -> value instanceof String;
            case SettingType.IntegerType __ -> value instanceof Number;
            case SettingType.DoubleType __ -> value instanceof Number;
            case SettingType.LongType __ -> value instanceof Number;
            case SettingType.BooleanType __ -> value instanceof Boolean;
            case SettingType.EnumType __ -> value instanceof String;
            case SettingType.ListType __ -> value instanceof List;
            case SettingType.MapType __ -> value instanceof java.util.Map;
            case SettingType.ColorType __ -> value instanceof String;
            case SettingType.DurationType __ -> value instanceof String;
            case SettingType.JsonType __ -> {
                try {
                    JsonParser.parseString(String.valueOf(value));
                    yield true;
                } catch (Exception e) {
                    yield false;
                }
            }
            case SettingType.PasswordType __ -> value instanceof String;
        };
        if (!typeOk) return false;
        if (value instanceof Number n) {
            double d = n.doubleValue();
            if (def.min() != null && d < def.min()) return false;
            if (def.max() != null && d > def.max()) return false;
        }
        if (def.options() != null && !def.options().isEmpty()) {
            if (!def.options().contains(String.valueOf(value))) return false;
        }
        return true;
    }

    public void loadFromConfig(SettingsRegistry registry) {
        for (String module : registry.getAllModules()) {
            for (var def : registry.getSchema().getOrDefault(module, List.of())) {
                String raw = configService.get("dashboard." + module, def.key());
                if (raw == null) raw = configService.get(module, def.key());
                if (raw == null) continue;
                try {
                    Object parsed = parseRawValue(raw, def);
                    if (validateValue(parsed, def)) {
                        registry.setSetting(module, def.key(), parsed, "config");
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load setting " + module + "/" + def.key() + ": " + e.getMessage());
                }
            }
        }
    }

    public void saveToConfig(SettingsRegistry registry) {
        for (String module : registry.getAllModules()) {
            for (var entry : registry.getSettings().getOrDefault(module, java.util.Map.of()).entrySet()) {
                Object value = entry.getValue();
                String raw = value != null ? value.toString() : "";
                configService.set("dashboard." + module, entry.getKey(), raw);
            }
        }
    }

    private Object parseRawValue(String raw, SettingDefinition def) {
        if (raw == null || raw.isEmpty()) return def.defaultValue();
        return switch (def.type()) {
            case SettingType.StringType __ -> raw;
            case SettingType.IntegerType __ -> {
                try { yield Integer.parseInt(raw); }
                catch (NumberFormatException e) { yield def.defaultValue(); }
            }
            case SettingType.DoubleType __ -> {
                try { yield Double.parseDouble(raw); }
                catch (NumberFormatException e) { yield def.defaultValue(); }
            }
            case SettingType.LongType __ -> {
                try { yield Long.parseLong(raw); }
                catch (NumberFormatException e) { yield def.defaultValue(); }
            }
            case SettingType.BooleanType __ -> Boolean.parseBoolean(raw);
            case SettingType.EnumType __ -> raw;
            case SettingType.ListType __ -> List.of(raw.split(","));
            case SettingType.MapType __ -> raw;
            case SettingType.ColorType __ -> raw;
            case SettingType.DurationType __ -> raw;
            case SettingType.JsonType __ -> raw;
            case SettingType.PasswordType __ -> raw;
        };
    }
}
