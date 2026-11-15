package org.voxelware.coretuff.Features.Security;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.voxelware.coretuff.CoreTuff;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

public class ProtocolCommandHider {

    private final CoreTuff plugin;
    private final SecurityConfig config;

    public ProtocolCommandHider(CoreTuff plugin) {
        this.plugin = plugin;
        this.config = plugin.getSecurityConfig();
    }

    public void register() {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        manager.addPacketListener(
                new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.COMMANDS) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        Player player = event.getPlayer();
                        if (!config.pluginHideEnabled()) return;
                        if (player.isOp() || player.hasPermission("coretuff.pl")) return;

                        try {
                            Object packet = event.getPacket().getHandle();
                            Field rootField = packet.getClass().getDeclaredFields()[0];
                            rootField.setAccessible(true);
                            Object root = rootField.get(packet);
                            Field childrenField = root.getClass().getSuperclass().getDeclaredField("children");
                            childrenField.setAccessible(true);
                            Map<String, ?> children = (Map<String, ?>) childrenField.get(root);

                            Iterator<? extends Map.Entry<String, ?>> iterator = children.entrySet().iterator();
                            while (iterator.hasNext()) {
                                Map.Entry<String, ?> entry = iterator.next();
                                String name = entry.getKey().toLowerCase();
                                if (name.contains(":")) {
                                    iterator.remove();
                                    continue;
                                }
                                Command command = Bukkit.getCommandMap().getCommand(name);
                                if (command == null || !isVisibleTo(player, command)) {
                                    iterator.remove();
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
        );
    }

    private boolean isVisibleTo(Player player, Command command) {
        String permission = command.getPermission();
        if (permission == null || permission.isBlank()) {
            return true;
        }
        return player.hasPermission(permission);
    }
}
