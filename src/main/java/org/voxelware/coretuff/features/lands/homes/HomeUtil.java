package org.voxelware.coretuff.features.lands.homes;

import org.bukkit.entity.Player;
import org.voxelware.coretuff.features.lands.LandConfig;

public class HomeUtil {

    public static int getMaxHomes(Player player, LandConfig config) {
        String base = config.homePermissionBase();
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission(base + i)) {
                return i;
            }
        }
        return config.maxHomesDefault();
    }
}
