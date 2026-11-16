package org.voxelware.coretuff.features.lands.warps.commands;

import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.voxelware.coretuff.features.lands.warps.WarpService;
import org.voxelware.coretuff.commandexecuter.PlayerCommand;

import java.util.List;

public class DelWarpCommand extends PlayerCommand {

    private final WarpService warpService;

    public DelWarpCommand(WarpService warpService) {
        this.warpService = warpService;
    }

    @Override
    public boolean onCommand(@NotNull Player player, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!player.hasPermission("coretuff.land.warps.delwarp")) {
            warpService.playSound(player, "warps.sounds.no-permission");
            player.sendMessage(warpService.plugin().format(player, "&cYou don't have permission to delete warps.", null));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(warpService.plugin().format(player, "&cUsage: /delwarp <name>", null));
            return true;
        }

        try {
            if (!warpService.warpExists(args[0])) {
                player.sendMessage(warpService.plugin().format(player, "&cWarp &f" + args[0] + " &cdoes not exist.", null));
                return true;
            }

            warpService.deleteWarp(args[0]);
            player.sendMessage(warpService.plugin().format(player, "&cWarp &f" + args[0] + " &cdeleted.", null));
            warpService.playSound(player, "warps.sounds.delete");
        } catch (Exception e) {
            player.sendMessage(warpService.plugin().format(player, "&cFailed to delete warp.", null));
        }
        return true;
    }

    @Override
    protected List<String> generateCompletions(org.bukkit.command.CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) return List.of();
        if (args.length == 1) {
            try {
                return warpService.getAllWarps().stream().map(w -> w.getName()).toList();
            } catch (Exception ignored) {}
        }
        return List.of();
    }
}
