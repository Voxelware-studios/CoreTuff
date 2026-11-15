package org.voxelware.coretuff.Features.Economy;

import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.database.config.ConfigurationService;
import org.voxelware.coretuff.database.config.DatabaseConfigProvider;

public final class EconomyConfig {

    private final DatabaseConfigProvider db;

    public EconomyConfig() {
        this.db = new DatabaseConfigProvider("economy");
    }

    public void setConfigurationService(ConfigurationService configService) {
        db.setConfigurationService(configService);
    }

    public void reload() {
        db.reload();
    }

    public FileConfiguration get() {
        return db.getConfig();
    }

    public String currencyName() {
        return db.getString("currency.name", "Dollar");
    }

    public String currencyNamePlural() {
        return db.getString("currency.name-plural", "Dollars");
    }

    public String currencySymbol() {
        return db.getString("currency.symbol", "$");
    }

    public int decimalPlaces() {
        return db.getInt("currency.decimal-places", 2);
    }

    public double startingBalance() {
        return db.getDouble("defaults.starting-balance", 0.0);
    }

    public double maxBalance() {
        return db.getDouble("defaults.max-balance", 999999999.99);
    }

    public double payTaxPercent() {
        return db.getDouble("transactions.pay-tax-percent", 0.0);
    }

    public double minimumTransfer() {
        return db.getDouble("transactions.minimum-transfer", 0.01);
    }

    public String taxBypassPermission() {
        return db.getString("transactions.pay-tax-bypass-permission", "coretuff.economy.bypass-tax");
    }

    public boolean interestEnabled() {
        return db.getBoolean("interest.enabled", false);
    }

    public double interestRatePercent() {
        return db.getDouble("interest.rate-percent", 0.5);
    }

    public int interestIntervalHours() {
        return db.getInt("interest.interval-hours", 24);
    }

    public double interestMaxBalance() {
        return db.getDouble("interest.max-balance", 100000.0);
    }

    public String interestBypassPermission() {
        return db.getString("interest.interest-bypass-permission", "coretuff.economy.bypass-interest");
    }
}
