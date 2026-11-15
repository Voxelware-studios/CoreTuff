package org.voxelware.coretuff.Addons.api;

import org.bukkit.entity.Player;

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
}
