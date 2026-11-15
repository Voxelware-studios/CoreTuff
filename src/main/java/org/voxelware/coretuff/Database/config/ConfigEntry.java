package org.voxelware.coretuff.database.config;

public final class ConfigEntry {
    private final String module;
    private final String key;
    private final String value;
    private final int version;
    private final long updatedAt;

    public ConfigEntry(String module, String key, String value, int version, long updatedAt) {
        this.module = module;
        this.key = key;
        this.value = value;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    public String getModule() { return module; }
    public String getKey() { return key; }
    public String getValue() { return value; }
    public int getVersion() { return version; }
    public long getUpdatedAt() { return updatedAt; }
}
