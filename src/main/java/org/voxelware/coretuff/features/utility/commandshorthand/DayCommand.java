package org.voxelware.coretuff.features.utility.commandshorthand;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.commandexecuter.BaseCommand;

import java.util.Collections;
import java.util.List;

public class DayCommand extends BaseCommand {

    private final CoreTuff plugin;

    public DayCommand(CoreTuff plugin) {
        this.plugin = plugin;
    }

    @Override
    protected List<String> generateCompletions(CommandSender sender, String label, String[] args) {
        if (args.length == 1) {
            return Bukkit.getWorlds().stream().map(World::getName).toList();
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 1) {
            return false;
        }

        if (!sender.hasPermission("coretuff.time.day")) {
            Player player = sender instanceof Player p ? p : null;
            sender.sendMessage(plugin.format(player, plugin.getCoreString("nopermission", "&cYou don't have permission to use this command."), null));
            return true;
        }

        final World targetWorld;

        if (args.length == 1) {
            targetWorld = Bukkit.getWorld(args[0]);

            if (targetWorld == null) {
                sender.sendMessage(createErrorComponent(
                        "World '%s' doesn't exist.", args[0]
                ));
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(createErrorComponent(
                        "Console must specify a world."
                ));
                return true;
            }

            targetWorld = player.getWorld();
        }

        Runnable setTimeTask = () -> {
            try {
                targetWorld.setTime(1000L);

            } catch (IllegalArgumentException e) {
                sender.sendMessage(createErrorComponent(
                        "World '%s' does not have a world clock.",
                        targetWorld.getName()
                ));
            }
        };

        if (plugin.isFolia()) {
            Bukkit.getGlobalRegionScheduler().run(plugin, task -> setTimeTask.run());
        } else {
            setTimeTask.run();
        }

        FileConfiguration config = plugin.getUtilityConfig().get();
        String msg = config != null ? config.getString("Time_Shorthand.day", "&aTime set to Day.") : "&aTime set to Day.";
        Player player = sender instanceof Player p ? p : null;
        sender.sendMessage(plugin.format(player, msg, null));
        return true;
    }
}

