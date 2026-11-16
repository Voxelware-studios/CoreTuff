package org.voxelware.coretuff.features.moderation.jail.commands;

import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.features.moderation.configengine.ModConfig;
import org.voxelware.coretuff.features.moderation.jail.Jail;
import org.voxelware.coretuff.features.moderation.jail.JailService;
import org.voxelware.coretuff.commandexecuter.PlayerCommand;

import java.util.List;

public class DelJailCommand extends PlayerCommand {

    private final JailService jailService;
    private final ModConfig config;
    private final CoreTuff plugin;

    public DelJailCommand(JailService jailService, ModConfig config, CoreTuff plugin) {
        this.jailService = jailService;
        this.config = config;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull Player player, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            player.sendMessage(plugin.format(player, config.jailMessage("usage-deljail", "&cUsage: /deljail <name>"), null));
            return true;
        }

        try {
            if (jailService.getJail(args[0]) == null) {
                player.sendMessage(plugin.format(player, config.jailMessage("no-jail-exists", "&cJail &f{jail} &cdoes not exist.").replace("{jail}", args[0]), null));
                return true;
            }

            jailService.deleteJail(args[0]);
            player.sendMessage(plugin.format(player, config.jailMessage("jail-deleted", "&cJail &f{jail} &cdeleted.").replace("{jail}", args[0]), null));
            jailService.playSound(player, "moderation.jail.sounds.delete");
        } catch (Exception e) {
            player.sendMessage(plugin.format(player, "&cFailed to delete jail.", null));
        }
        return true;
    }

    @Override
    protected List<String> generateCompletions(org.bukkit.command.CommandSender sender, String label, String[] args) {
        if (args.length == 1) {
            try {
                return jailService.getAllJails().stream().map(Jail::getName).toList();
            } catch (Exception ignored) {}
        }
        return List.of();
    }
}
