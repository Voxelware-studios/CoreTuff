package org.voxelware.coretuff.Utility;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class CoreConfigLoader {

	private final VersionedConfigLoader loader;
	
	public CoreConfigLoader(JavaPlugin plugin) {
		loader = new VersionedConfigLoader(plugin, "config.yml", "Core/config.yml");
	}
	
	public FileConfiguration getConfig() {
		return loader.getConfig();
	}

	public void reload() {
		loader.reload();
	}
	
	public void save() {
		loader.save();
	}

	public void saveConfig() {
		loader.save();
	}
}
