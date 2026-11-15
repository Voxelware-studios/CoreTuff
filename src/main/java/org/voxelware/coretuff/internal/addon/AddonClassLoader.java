package org.voxelware.coretuff.internal.addon;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class AddonClassLoader extends URLClassLoader {

    private final String addonId;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public AddonClassLoader(String addonId, Path jarPath, ClassLoader parent) {
        super(new URL[]{}, parent);
        this.addonId = addonId;
        addJar(jarPath);
    }

    public String addonId() { return addonId; }

    private void addJar(Path path) {
        try {
            addURL(path.toUri().toURL());
        } catch (Exception e) {
            throw new RuntimeException("Failed to add URL for addon: " + addonId, e);
        }
    }
}
