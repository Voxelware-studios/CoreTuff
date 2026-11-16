package org.voxelware.coretuff.features.lands.homes.commands;

import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.voxelware.coretuff.features.lands.homes.Home;
import org.voxelware.coretuff.features.lands.homes.HomeRepository;
import org.voxelware.coretuff.features.lands.LandConfig;
import org.voxelware.coretuff.utility.ConfigSoundPlayer;
import org.voxelware.coretuff.commandexecuter.PlayerCommand;

import java.util.List;

public class DelHomeCommand extends PlayerCommand {

    private final HomeRepository repository;
    private final LandConfig config;
    private final org.voxelware.coretuff.CoreTuff plugin;

    public DelHomeCommand(HomeRepository repository, LandConfig config, org.voxelware.coretuff.CoreTuff plugin) {
        this.repository = repository;
        this.config = config;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull Player player, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            player.sendMessage(plugin.format(player, "&cUsage: /delhome <name>", null));
            return true;
        }

        try {
            Home home = repository.getHome(player.getUniqueId(), args[0]);
            if (home == null) {
                player.sendMessage(plugin.format(player, "&cHome &f" + args[0] + " &cdoes not exist.", null));
                return true;
            }

            repository.deleteHome(player.getUniqueId(), args[0]);
            player.sendMessage(plugin.format(player, "&cHome &f" + args[0] + " &cdeleted.", null));
            playSound(player, "homes.sounds.delete");
        } catch (Exception e) {
            player.sendMessage(plugin.format(player, "&cFailed to delete home.", null));
        }
        return true;
    }

    private void playSound(Player player, String path) {
        if (player == null || !player.isOnline()) return;
        ConfigSoundPlayer.play(plugin, player, config.get(), path);
    }

    @Override
    protected List<String> generateCompletions(org.bukkit.command.CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) return List.of();
        if (args.length == 1) {
            try {
                return repository.getHomes(player.getUniqueId()).stream().map(Home::getName).toList();
            } catch (Exception ignored) {}
        }
        return List.of();
    }
}
