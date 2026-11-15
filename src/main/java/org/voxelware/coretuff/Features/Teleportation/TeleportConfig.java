package org.voxelware.coretuff.Features.Teleportation;

import org.bukkit.configuration.file.FileConfiguration;
import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.Utility.VersionedConfigLoader;

public class TeleportConfig {

	private final VersionedConfigLoader loader;

	public TeleportConfig(CoreTuff plugin) {
		loader = new VersionedConfigLoader(plugin, "teleportconfig.yml", "Core/teleportconfig.yml");
	}

	public void reload() {
		loader.reload();
	}

	public FileConfiguration get() {
		return loader.getConfig();
	}
}
