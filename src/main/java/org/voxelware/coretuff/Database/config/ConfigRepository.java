package org.voxelware.coretuff.database.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigRepository {

    private final ConfigDatabase database;

    public ConfigRepository(ConfigDatabase database) {
        this.database = database;
    }

    public ConfigEntry find(String module, String key) throws SQLException {
        String sql = "SELECT module, config_key, config_value, version, updated_at FROM config_entries WHERE module = ? AND config_key = ?";
        try (PreparedStatement stmt = database.getConnection().prepareStatement(sql)) {
            stmt.setString(1, module);
            stmt.setString(2, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<ConfigEntry> findByModule(String module) throws SQLException {
        String sql = "SELECT module, config_key, config_value, version, updated_at FROM config_entries WHERE module = ?";
        List<ConfigEntry> results = new ArrayList<>();
        try (PreparedStatement stmt = database.getConnection().prepareStatement(sql)) {
            stmt.setString(1, module);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    public List<ConfigEntry> findAll() throws SQLException {
        String sql = "SELECT module, config_key, config_value, version, updated_at FROM config_entries ORDER BY module, config_key";
        List<ConfigEntry> results = new ArrayList<>();
        try (PreparedStatement stmt = database.getConnection().prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    public void upsert(String module, String key, String value, long now) throws SQLException {
        String sql = """
            MERGE INTO config_entries (module, config_key, config_value, version, updated_at)
            KEY(module, config_key)
            VALUES (?, ?, ?, (SELECT COALESCE(version, 0) + 1 FROM config_entries WHERE module = ? AND config_key = ?), ?)
        """;
        try (PreparedStatement stmt = database.getConnection().prepareStatement(sql)) {
            stmt.setString(1, module);
            stmt.setString(2, key);
            stmt.setString(3, value);
            stmt.setString(4, module);
            stmt.setString(5, key);
            stmt.setLong(6, now);
            stmt.executeUpdate();
        }
    }

    public void delete(String module, String key) throws SQLException {
        String sql = "DELETE FROM config_entries WHERE module = ? AND config_key = ?";
        try (PreparedStatement stmt = database.getConnection().prepareStatement(sql)) {
            stmt.setString(1, module);
            stmt.setString(2, key);
            stmt.executeUpdate();
        }
    }

    public void deleteModule(String module) throws SQLException {
        String sql = "DELETE FROM config_entries WHERE module = ?";
        try (PreparedStatement stmt = database.getConnection().prepareStatement(sql)) {
            stmt.setString(1, module);
            stmt.executeUpdate();
        }
    }

    public void batchUpsert(List<ConfigEntry> entries) throws SQLException {
        String sql = """
            MERGE INTO config_entries (module, config_key, config_value, version, updated_at)
            KEY(module, config_key)
            VALUES (?, ?, ?, (SELECT COALESCE(version, 0) + 1 FROM config_entries WHERE module = ? AND config_key = ?), ?)
        """;
        Connection conn = database.getConnection();
        boolean origAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (ConfigEntry entry : entries) {
                stmt.setString(1, entry.getModule());
                stmt.setString(2, entry.getKey());
                stmt.setString(3, entry.getValue());
                stmt.setString(4, entry.getModule());
                stmt.setString(5, entry.getKey());
                stmt.setLong(6, entry.getUpdatedAt());
                stmt.addBatch();
            }
            stmt.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(origAutoCommit);
        }
    }

    public boolean isMigrated() throws SQLException {
        String sql = "SELECT migrated FROM config_metadata WHERE schema_version = 1";
        try (PreparedStatement stmt = database.getConnection().prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean("migrated");
            }
        }
    }

    public void markMigrated(String source) throws SQLException {
        String sql = "UPDATE config_metadata SET migrated = TRUE, migrated_at = ?, migration_source = ? WHERE schema_version = 1";
        try (PreparedStatement stmt = database.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setString(2, source);
            stmt.executeUpdate();
        }
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM config_entries";
        try (PreparedStatement stmt = database.getConnection().prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private ConfigEntry mapRow(ResultSet rs) throws SQLException {
        return new ConfigEntry(
                rs.getString("module"),
                rs.getString("config_key"),
                rs.getString("config_value"),
                rs.getInt("version"),
                rs.getLong("updated_at")
        );
    }
}
