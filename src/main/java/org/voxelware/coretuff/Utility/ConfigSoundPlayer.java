package org.voxelware.coretuff.Utility;


import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.voxelware.coretuff.CoreTuff;

import java.util.Locale;

public final class ConfigSoundPlayer {

	private ConfigSoundPlayer() {
	}

	public static void play(CoreTuff plugin, Player player, FileConfiguration config, String path) {
		if (plugin == null || player == null || !player.isOnline() || config == null) {
			return;
		}

		if (!config.getBoolean(path + ".enabled", false)) {
			return;
		}

		String soundName = config.getString(path + ".sound");
		if (soundName == null || soundName.isBlank()) {
			return;
		}

		try {
			Sound sound = Sound.valueOf(soundName.toUpperCase(Locale.ROOT));
			float volume = (float) config.getDouble(path + ".volume", 1.0D);
			float pitch = (float) config.getDouble(path + ".pitch", 1.0D);
			player.playSound(player.getLocation(), sound, volume, pitch);
		} catch (IllegalArgumentException exception) {
			plugin.getLogger().warning("Invalid configured sound at " + path + ": " + soundName);
		}
	}
}
