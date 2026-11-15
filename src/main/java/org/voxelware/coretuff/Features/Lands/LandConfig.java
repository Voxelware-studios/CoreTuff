package org.voxelware.coretuff.Features.Lands;

import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.Utility.VersionedConfigLoader;

public final class LandConfig {

    private final VersionedConfigLoader loader;

    public LandConfig(CoreTuff plugin) {
        loader = new VersionedConfigLoader(plugin, "landconfig.yml", "Core/landconfig.yml");
    }

    public void reload() {
        loader.reload();
    }

    public FileConfiguration get() {
        return loader.getConfig();
    }

    // Homes
    public int homeTeleportDelay() {
        return get().getInt("homes.teleport-delay-seconds", 3);
    }

    public int maxHomesDefault() {
        return get().getInt("homes.max-homes-default", 1);
    }

    public String homePermissionBase() {
        return get().getString("homes.max-homes-permission-base", "coretuff.land.sethome.");
    }

    // Warps
    public int warpTeleportDelay() {
        return get().getInt("warps.teleport-delay-seconds", 3);
    }

    public double warpCost() {
        return get().getDouble("warps.economy-cost", 0.0);
    }

    public String warpCostBypassPermission() {
        return get().getString("warps.economy-cost-permission-bypass", "coretuff.land.warps.bypass-cost");
    }

    public String warpPermissionBase() {
        return get().getString("warps.max-warps-permission-base", "coretuff.land.warps.setwarp.");
    }

    // Warp permission keys
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

    // Message helpers
    public String message(String path, String fallback) {
        return get().getString(path, fallback);
    }
}
