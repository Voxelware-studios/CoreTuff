package org.voxelware.coretuff.database.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Logger;

public final class MigrationService {

    private static final Logger LOG = Logger.getLogger(MigrationService.class.getName());

    private final JavaPlugin plugin;
    private final ConfigurationService configService;
    private final ConfigRepository repository;

    public MigrationService(JavaPlugin plugin, ConfigurationService configService, ConfigRepository repository) {
        this.plugin = plugin;
        this.configService = configService;
        this.repository = repository;
    }

    public void migrateIfNeeded() throws SQLException {
        if (repository.isMigrated()) {
            LOG.info("Configuration database already initialized.");
            return;
        }

        repository.markMigrated("database");
        LOG.info("Configuration database initialized.");
    }
}
