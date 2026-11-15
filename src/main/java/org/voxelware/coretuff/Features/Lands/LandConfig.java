package org.voxelware.coretuff.Features.Lands;

import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.database.config.ConfigurationService;
import org.voxelware.coretuff.database.config.DatabaseConfigProvider;

public final class LandConfig {

    private final DatabaseConfigProvider db;

    public LandConfig() {
        this.db = new DatabaseConfigProvider("lands");
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

    public int homeTeleportDelay() {
        return db.getInt("homes.teleport-delay-seconds", 3);
    }

    public int maxHomesDefault() {
        return db.getInt("homes.max-homes-default", 1);
    }

    public String homePermissionBase() {
        return db.getString("homes.max-homes-permission-base", "coretuff.land.sethome.");
    }

    public int warpTeleportDelay() {
        return db.getInt("warps.teleport-delay-seconds", 3);
    }

    public double warpCost() {
        return db.getDouble("warps.economy-cost", 0.0);
    }

    public String warpCostBypassPermission() {
        return db.getString("warps.economy-cost-permission-bypass", "coretuff.land.warps.bypass-cost");
    }

    public String warpPermissionBase() {
        return db.getString("warps.max-warps-permission-base", "coretuff.land.warps.setwarp.");
    }

    public String warpSetPermission() {
        return "coretuff.land.warps.setwarp";
    }

    public String warpDeletePermission() {
        return "coretuff.land.warps.delwarp";
    }

    public String warpListPermission() {
        return "coretuff.land.warps.list";
    }

    public String warpOthersPermission() {
        return "coretuff.land.warps.warp.others";
    }

    public String message(String path, String fallback) {
        return db.getString(path, fallback);
    }
}
