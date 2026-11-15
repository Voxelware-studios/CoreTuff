package org.voxelware.coretuff.Features.Economy;

import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.Utility.VersionedConfigLoader;

public final class EconomyConfig {

    private final VersionedConfigLoader loader;

    public EconomyConfig(CoreTuff plugin) {
        loader = new VersionedConfigLoader(plugin, "ecoconfig.yml", "Core/ecoconfig.yml");
    }

    public void reload() {
        loader.reload();
    }

    public FileConfiguration get() {
        return loader.getConfig();
    }

    public String currencyName() {
        return get().getString("currency.name", "Dollar");
    }

    public String currencyNamePlural() {
        return get().getString("currency.name-plural", "Dollars");
    }

    public String currencySymbol() {
        return get().getString("currency.symbol", "$");
    }

    public int decimalPlaces() {
        return get().getInt("currency.decimal-places", 2);
    }

    public double startingBalance() {
        return get().getDouble("defaults.starting-balance", 0.0);
    }

    public double maxBalance() {
        return get().getDouble("defaults.max-balance", 999999999.99);
    }

    public double payTaxPercent() {
        return get().getDouble("transactions.pay-tax-percent", 0.0);
    }

    public double minimumTransfer() {
        return get().getDouble("transactions.minimum-transfer", 0.01);
    }

    public String taxBypassPermission() {
        return get().getString("transactions.pay-tax-bypass-permission", "coretuff.economy.bypass-tax");
    }

    public boolean interestEnabled() {
        return get().getBoolean("interest.enabled", false);
    }

    public double interestRatePercent() {
        return get().getDouble("interest.rate-percent", 0.5);
    }

    public int interestIntervalHours() {
        return get().getInt("interest.interval-hours", 24);
    }

    public double interestMaxBalance() {
        return get().getDouble("interest.max-balance", 100000.0);
    }

    public String interestBypassPermission() {
        return get().getString("interest.interest-bypass-permission", "coretuff.economy.bypass-interest");
    }
}
