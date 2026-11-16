package org.voxelware.coretuff.addons.api;

import org.bukkit.entity.Player;
import org.voxelware.coretuff.features.teleportation.DelayedTeleporter;
import org.voxelware.coretuff.features.teleportation.handler.ExecutionHandler;
import org.voxelware.coretuff.features.teleportation.handler.TeleportHandler;
import org.voxelware.coretuff.features.teleportation.TpManager;
import org.voxelware.coretuff.api.economy.EconomyApi;
import org.voxelware.coretuff.api.economy.InterestApi;
import org.voxelware.coretuff.api.economy.TransactionApi;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

public interface CoreTuffAddonContext {

	String addonName();

	String addonVersion();

	String coreVersion();

	boolean isFolia();

	File addonDataFolder();

	File addonJar();

	Logger logger();

	Map<String, Object> configSnapshot(String configName);

	void sendMessage(Player player, String message, Map<String, String> placeholders);

	ExecutionHandler executionHandler();

	TeleportHandler teleportHandler();

	DelayedTeleporter delayedTeleporter();

	TpManager tpManager();

	EconomyApi getEconomyApi();

	TransactionApi getTransactionApi();

	InterestApi getInterestApi();
}
