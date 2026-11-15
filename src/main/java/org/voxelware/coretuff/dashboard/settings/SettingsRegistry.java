package org.voxelware.coretuff.dashboard.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SettingsRegistry {

    private final Map<String, Map<String, SettingEntry>> moduleSettings = new ConcurrentHashMap<>();
    private final Map<String, SettingsCategory> moduleCategories = new ConcurrentHashMap<>();
    private final List<SettingChangeListener> listeners = new CopyOnWriteArrayList<>();

    public sealed interface SettingType permits
            SettingType.StringType, SettingType.IntegerType, SettingType.DoubleType,
            SettingType.LongType, SettingType.BooleanType, SettingType.EnumType,
            SettingType.ListType, SettingType.MapType, SettingType.ColorType,
            SettingType.DurationType, SettingType.JsonType, SettingType.PasswordType {

        record StringType() implements SettingType {}
        record IntegerType() implements SettingType {}
        record DoubleType() implements SettingType {}
        record LongType() implements SettingType {}
        record BooleanType() implements SettingType {}
        record EnumType() implements SettingType {}
        record ListType() implements SettingType {}
        record MapType() implements SettingType {}
        record ColorType() implements SettingType {}
        record DurationType() implements SettingType {}
        record JsonType() implements SettingType {}
        record PasswordType() implements SettingType {}
    }

    public record SettingDefinition(
            String key,
            SettingType type,
            Object defaultValue,
            Double min,
            Double max,
            String description,
            List<String> options,
            String category
    ) {}

    public record SettingsCategory(String name, String displayName, int order) {}

    public record SettingEntry(SettingDefinition definition, Object value, String setBy) {}

    public void register(String moduleName, SettingsCategory category, List<SettingDefinition> settings) {
        if (category != null) moduleCategories.put(moduleName, category);
        Map<String, SettingEntry> moduleMap = moduleSettings
                .computeIfAbsent(moduleName, k -> new ConcurrentHashMap<>());
        for (var def : settings) {
            moduleMap.putIfAbsent(def.key(), new SettingEntry(def, def.defaultValue(), "system"));
        }
    }

    public void unregister(String moduleName) {
        moduleSettings.remove(moduleName);
        moduleCategories.remove(moduleName);
    }

    public Map<String, List<SettingDefinition>> getSchema() {
        Map<String, List<SettingDefinition>> schema = new ConcurrentHashMap<>();
        for (var entry : moduleSettings.entrySet()) {
            String module = entry.getKey();
            List<SettingDefinition> defs = new ArrayList<>();
            for (var setting : entry.getValue().values()) {
                defs.add(setting.definition());
            }
            schema.put(module, defs);
        }
        return schema;
    }

    public Map<String, Map<String, Object>> getSettings() {
        Map<String, Map<String, Object>> all = new ConcurrentHashMap<>();
        for (var moduleEntry : moduleSettings.entrySet()) {
            Map<String, Object> moduleValues = new ConcurrentHashMap<>();
            for (var setting : moduleEntry.getValue().values()) {
                moduleValues.put(setting.definition().key(), setting.value());
            }
            all.put(moduleEntry.getKey(), moduleValues);
        }
        return all;
    }

    public SettingEntry getSetting(String moduleName, String key) {
        Map<String, SettingEntry> module = moduleSettings.get(moduleName);
        if (module == null) return null;
        return module.get(key);
    }

    public boolean setSetting(String moduleName, String key, Object value, String actor) {
        Map<String, SettingEntry> module = moduleSettings.get(moduleName);
        if (module == null) return false;
        SettingEntry entry = module.get(key);
        if (entry == null) return false;
        Object oldValue = entry.value();
        SettingEntry newEntry = new SettingEntry(entry.definition(), value, actor);
        module.put(key, newEntry);
        for (var listener : listeners) {
            try {
                listener.onSettingChanged(moduleName, key, oldValue, value, actor);
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    public List<SettingsCategory> getAllCategories() {
        List<SettingsCategory> cats = new ArrayList<>(moduleCategories.values());
        cats.sort((a, b) -> Integer.compare(a.order(), b.order()));
        return Collections.unmodifiableList(cats);
    }

    public Set<String> getAllModules() {
        return Collections.unmodifiableSet(moduleSettings.keySet());
    }

    public void addListener(SettingChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SettingChangeListener listener) {
        listeners.remove(listener);
    }
}
