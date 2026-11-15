package org.voxelware.coretuff.dashboard.settings;

@FunctionalInterface
public interface SettingChangeListener {

    void onSettingChanged(String module, String key, Object oldValue, Object newValue, String actor);

    default void onReload() {}
}
