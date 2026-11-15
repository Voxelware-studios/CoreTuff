package org.voxelware.coretuff.api.addon;

public interface AddonLifecycle {

    void onLoad(AddonContext context);

    void onEnable(AddonContext context);

    default void onDisable() {}

    default void onUnload() {}
}
