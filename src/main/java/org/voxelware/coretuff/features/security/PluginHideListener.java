package org.voxelware.coretuff.features.security;

import com.destroystokyo.paper.event.brigadier.AsyncPlayerSendCommandsEvent;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.voxelware.coretuff.CoreTuff;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PluginHideListener implements Listener {

    private static final List<String> BLOCKED_COMMANDS = List.of(
            "/pl", "/plugins", "/bukkit:pl", "/bukkit:plugins",
            "/?", "/help"
    );

    private static final Field CHILDREN_FIELD;
    static {
        Field field = null;
        try {
            field = CommandNode.class.getDeclaredField("children");
            field.setAccessible(true);
        } catch (NoSuchFieldException ignored) {}
        CHILDREN_FIELD = field;
    }

    private final CoreTuff plugin;
    private final SecurityConfig config;

    public PluginHideListener(CoreTuff plugin) {
        this.plugin = plugin;
        this.config = plugin.getSecurityConfig();
    }

    private boolean canBypass(Player player) {
        return player.isOp() || player.hasPermission("coretuff.pl");
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!config.pluginHideEnabled()) return;
        Player player = event.getPlayer();
        if (canBypass(player)) return;

        String message = event.getMessage().toLowerCase();
        for (String blocked : BLOCKED_COMMANDS) {
            if (message.equals(blocked) || message.startsWith(blocked + " ")) {
                event.setCancelled(true);
                player.sendMessage(plugin.format(player, config.hiddenCommandMessage(), null));
                return;
            }
        }
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        if (!config.pluginHideEnabled()) return;
        Player player = event.getPlayer();
        if (canBypass(player)) return;

        event.getCommands().removeIf(commandName -> {
            Command command = Bukkit.getCommandMap().getCommand(commandName);
            return command == null || !isVisibleTo(player, command);
        });
    }

    @EventHandler
    public void onTabComplete(AsyncTabCompleteEvent event) {
        if (!config.pluginHideEnabled()) return;
        if (!(event.getSender() instanceof Player player)) return;
        if (canBypass(player)) return;

        String buffer = event.getBuffer();
        if (!buffer.startsWith("/")) return;
        String input = buffer.substring(1).toLowerCase();

        List<AsyncTabCompleteEvent.Completion> completions = new ArrayList<>();
        for (Map.Entry<String, Command> entry : Bukkit.getCommandMap().getKnownCommands().entrySet()) {
            String name = entry.getKey();
            if (name.contains(":")) continue;
            Command command = entry.getValue();
            if (!isVisibleTo(player, command)) continue;
            if (!name.toLowerCase().startsWith(input)) continue;
            completions.add(AsyncTabCompleteEvent.Completion.completion(name));
        }

        event.completions().clear();
        event.completions().addAll(completions);
        event.setHandled(true);
    }

    @EventHandler
    @SuppressWarnings("unchecked")
    public void onSendCommands(AsyncPlayerSendCommandsEvent<?> event) {
        if (!config.pluginHideEnabled()) return;
        Player player = event.getPlayer();
        if (canBypass(player)) return;

        if (CHILDREN_FIELD == null) return;

        RootCommandNode<?> root = event.getCommandNode();
        try {
            Map<String, CommandNode<?>> children = (Map<String, CommandNode<?>>) CHILDREN_FIELD.get(root);
            List<String> toRemove = new ArrayList<>();

            for (Map.Entry<String, CommandNode<?>> entry : children.entrySet()) {
                String name = entry.getKey();
                if (name.contains(":")) {
                    toRemove.add(name);
                    continue;
                }
                Command command = Bukkit.getCommandMap().getCommand(name);
                if (command == null || !isVisibleTo(player, command)) {
                    toRemove.add(name);
                }
            }

            toRemove.forEach(children::remove);
        } catch (Exception ignored) {}
    }

    private boolean isVisibleTo(Player player, Command command) {
        String permission = command.getPermission();
        if (permission == null || permission.isBlank()) {
            return true;
        }
        return player.hasPermission(permission);
    }
}
