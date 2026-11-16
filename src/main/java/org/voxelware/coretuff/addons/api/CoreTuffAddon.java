package org.voxelware.coretuff.addons.api;

public interface CoreTuffAddon {

	default void onLoad(CoreTuffAddonContext context) throws Exception {
	}

	default void onEnable(CoreTuffAddonContext context) throws Exception {
	}

	default void onDisable(CoreTuffAddonContext context) throws Exception {
	}
}
