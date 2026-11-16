package org.voxelware.coretuff.database.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public final class ConfigDatabase {

    private static final Logger LOG = Logger.getLogger(ConfigDatabase.class.getName());

    private Connection connection;

    public void connect(JavaPlugin plugin) throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("H2 Driver not found!", e);
        }

        File coreFolder = new File(plugin.getDataFolder(), "Core");
        if (!coreFolder.exists()) coreFolder.mkdirs();

        String url = "jdbc:h2:" + new File(coreFolder, "config").getAbsolutePath()
                + ";TRACE_LEVEL_FILE=0";

        connection = DriverManager.getConnection(url);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS config_entries (
                    id IDENTITY PRIMARY KEY,
                    module VARCHAR(255) NOT NULL,
                    config_key VARCHAR(255) NOT NULL,
                    config_value TEXT,
                    version INT DEFAULT 1,
                    updated_at BIGINT NOT NULL,
                    UNIQUE(module, config_key)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS config_metadata (
                    schema_version INT DEFAULT 1,
                    migrated BOOLEAN DEFAULT FALSE,
                    migrated_at BIGINT,
                    migration_source VARCHAR(255)
                )
            """);

            stmt.execute("""
                MERGE INTO config_metadata (schema_version, migrated, migrated_at, migration_source)
                KEY(schema_version)
                VALUES (1, FALSE, NULL, NULL)
            """);
        }

        LOG.info("Configuration database connected.");
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOG.warning("Error closing config database: " + e.getMessage());
            }
        }
    }
}
