package org.voxelware.coretuff.database.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public final class ConfigurationService {

    private static final Logger LOG = Logger.getLogger(ConfigurationService.class.getName());

    private final ConfigDatabase database;
    private final ConfigRepository repository;
    private final MigrationService migrationService;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<String, Map<String, CachedValue>> cache = new ConcurrentHashMap<>();

    private record CachedValue(String value, int version, long updatedAt) {}

    public ConfigurationService(JavaPlugin plugin) {
        this.database = new ConfigDatabase();
        this.repository = new ConfigRepository(database);
        this.migrationService = new MigrationService(plugin, this, repository);
    }

    public void init() throws Exception {
        database.connect(JavaPlugin.getProvidingPlugin(getClass()));
        migrationService.migrateIfNeeded();
        loadCache();
        LOG.info("ConfigurationService initialized with " + cache.size() + " modules");
    }

    private void loadCache() throws SQLException {
        lock.writeLock().lock();
        try {
            cache.clear();
            List<ConfigEntry> all = repository.findAll();
            for (ConfigEntry entry : all) {
                cache.computeIfAbsent(entry.getModule(), k -> new ConcurrentHashMap<>())
                        .put(entry.getKey(), new CachedValue(entry.getValue(), entry.getVersion(), entry.getUpdatedAt()));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String get(String module, String key) {
        lock.readLock().lock();
        try {
            Map<String, CachedValue> moduleCache = cache.get(module);
            if (moduleCache == null) return null;
            CachedValue cv = moduleCache.get(key);
            return cv != null ? cv.value() : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public String get(String module, String key, String defaultValue) {
        String val = get(module, key);
        return val != null ? val : defaultValue;
    }

    public int getInt(String module, String key, int defaultValue) {
        String val = get(module, key);
        if (val == null) return defaultValue;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return defaultValue; }
    }

    public double getDouble(String module, String key, double defaultValue) {
        String val = get(module, key);
        if (val == null) return defaultValue;
        try { return Double.parseDouble(val); } catch (NumberFormatException e) { return defaultValue; }
    }

    public boolean getBoolean(String module, String key, boolean defaultValue) {
        String val = get(module, key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val);
    }

    public Map<String, String> getModule(String module) {
        lock.readLock().lock();
        try {
            Map<String, CachedValue> moduleCache = cache.get(module);
            if (moduleCache == null) return Map.of();
            Map<String, String> result = new LinkedHashMap<>();
            for (var entry : moduleCache.entrySet()) {
                result.put(entry.getKey(), entry.getValue().value());
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, Map<String, String>> getAll() {
        lock.readLock().lock();
        try {
            Map<String, Map<String, String>> result = new LinkedHashMap<>();
            for (var moduleEntry : cache.entrySet()) {
                Map<String, String> moduleMap = new LinkedHashMap<>();
                for (var entry : moduleEntry.getValue().entrySet()) {
                    moduleMap.put(entry.getKey(), entry.getValue().value());
                }
                result.put(moduleEntry.getKey(), moduleMap);
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean set(String module, String key, String value) {
        long now = System.currentTimeMillis();
        lock.writeLock().lock();
        try {
            repository.upsert(module, key, value, now);
            int newVersion = 1;
            Map<String, CachedValue> moduleCache = cache.computeIfAbsent(module, k -> new ConcurrentHashMap<>());
            CachedValue existing = moduleCache.get(key);
            if (existing != null) newVersion = existing.version() + 1;
            moduleCache.put(key, new CachedValue(value, newVersion, now));
            return true;
        } catch (SQLException e) {
            LOG.warning("Failed to set config " + module + "/" + key + ": " + e.getMessage());
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public BatchResult setBatch(Map<String, Map<String, String>> changes) {
        long now = System.currentTimeMillis();
        lock.writeLock().lock();
        try {
            boolean origAutoCommit = database.getConnection().getAutoCommit();
            database.getConnection().setAutoCommit(false);
            try {
                List<ConfigEntry> entries = new ArrayList<>();
                for (var moduleEntry : changes.entrySet()) {
                    String module = moduleEntry.getKey();
                    for (var entry : moduleEntry.getValue().entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        repository.upsert(module, key, value, now);
                        int newVersion = 1;
                        Map<String, CachedValue> moduleCache = cache.computeIfAbsent(module, k -> new ConcurrentHashMap<>());
                        CachedValue existing = moduleCache.get(key);
                        if (existing != null) newVersion = existing.version() + 1;
                        moduleCache.put(key, new CachedValue(value, newVersion, now));
                    }
                }
                database.getConnection().commit();
                return new BatchResult(true, null);
            } catch (SQLException e) {
                database.getConnection().rollback();
                LOG.warning("Batch config update failed, rolled back: " + e.getMessage());
                return new BatchResult(false, e.getMessage());
            } finally {
                database.getConnection().setAutoCommit(origAutoCommit);
            }
        } catch (SQLException e) {
            return new BatchResult(false, e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void delete(String module, String key) {
        lock.writeLock().lock();
        try {
            repository.delete(module, key);
            Map<String, CachedValue> moduleCache = cache.get(module);
            if (moduleCache != null) moduleCache.remove(key);
        } catch (SQLException e) {
            LOG.warning("Failed to delete config " + module + "/" + key + ": " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void invalidateCache() {
        try {
            loadCache();
        } catch (SQLException e) {
            LOG.warning("Failed to reload config cache: " + e.getMessage());
        }
    }

    public String exportAsJson() {
        var all = getAll();
        var gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(all);
    }

    public BatchResult importFromJson(String json) {
        try {
            var gson = new com.google.gson.Gson();
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> data = gson.fromJson(json, Map.class);
            Map<String, Map<String, String>> converted = new LinkedHashMap<>();
            for (var moduleEntry : data.entrySet()) {
                Map<String, String> moduleMap = new LinkedHashMap<>();
                for (var entry : moduleEntry.getValue().entrySet()) {
                    moduleMap.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
                }
                converted.put(moduleEntry.getKey(), moduleMap);
            }
            return setBatch(converted);
        } catch (Exception e) {
            return new BatchResult(false, "Failed to parse JSON: " + e.getMessage());
        }
    }

    public void shutdown() {
        database.close();
    }

    public ConfigRepository getRepository() {
        return repository;
    }

    public MigrationService getMigrationService() {
        return migrationService;
    }

    public record BatchResult(boolean success, String error) {}
}
