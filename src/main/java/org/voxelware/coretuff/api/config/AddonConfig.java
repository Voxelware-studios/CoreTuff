package org.voxelware.coretuff.api.config;

import java.util.List;
import java.util.Map;

public interface AddonConfig {

    String name();

    String path();

    void reload();

    void save();

    boolean contains(String path);

    String getString(String path);

    String getString(String path, String fallback);

    int getInt(String path);

    int getInt(String path, int fallback);

    double getDouble(String path);

    double getDouble(String path, double fallback);

    boolean getBoolean(String path);

    boolean getBoolean(String path, boolean fallback);

    List<String> getStringList(String path);

    Map<String, Object> getValues(boolean deep);

    void set(String path, Object value);

    int configVersion();

    boolean migrate(int targetVersion, String targetSection);
}
