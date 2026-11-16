package org.voxelware.coretuff.features.teleportation.handler;

import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface TeleportHandler {
	CompletableFuture<Boolean> teleport(Player paramPlayer, Location paramLocation);
}