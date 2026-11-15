package org.voxelware.coretuff.internal.addon;

import org.voxelware.coretuff.api.addon.*;
import org.voxelware.coretuff.internal.logging.AddonLoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class AddonLoaderImpl {

    private static final Logger LOG = Logger.getLogger(AddonLoaderImpl.class.getName());

    private final Map<String, LoadedAddon> loaded = new LinkedHashMap<>();
    private final ClassLoader platformClassLoader;

    public AddonLoaderImpl(ClassLoader platformClassLoader) {
        this.platformClassLoader = platformClassLoader;
    }

    public static final class LoadedAddon {
        private final AddonManifest manifest;
        private final AddonClassLoader classLoader;
        private final AddonLifecycle instance;
        private final AddonContext context;

        public LoadedAddon(AddonManifest manifest, AddonClassLoader classLoader,
                           AddonLifecycle instance, AddonContext context) {
            this.manifest = manifest;
            this.classLoader = classLoader;
            this.instance = instance;
            this.context = context;
        }

        public AddonManifest manifest() { return manifest; }
        public AddonClassLoader classLoader() { return classLoader; }
        public AddonLifecycle instance() { return instance; }
        public AddonContext context() { return context; }
    }

    public AddonLoadResult load(Path jarPath) {
        try {
            if (!Files.exists(jarPath)) {
                return AddonLoadResult.failure(
                        null, List.of("Jar not found: " + jarPath));
            }

            AddonManifest manifest = ManifestParser.parse(jarPath);
            if (manifest == null) {
                return AddonLoadResult.failure(
                        null, List.of("Missing addon.yml in " + jarPath));
            }

            if (loaded.containsKey(manifest.id())) {
                return AddonLoadResult.failure(
                        manifest, List.of("Addon '" + manifest.id() + "' is already loaded"));
            }

            String mainClass = findMainClass(jarPath);
            if (mainClass == null) {
                return AddonLoadResult.failure(
                        manifest, List.of("No main class found in " + jarPath));
            }

            AddonClassLoader classLoader = new AddonClassLoader(manifest.id(), jarPath, platformClassLoader);

            Class<?> mainClazz = Class.forName(mainClass, true, classLoader);
            if (!AddonLifecycle.class.isAssignableFrom(mainClazz)) {
                return AddonLoadResult.failure(
                        manifest, List.of("Main class " + mainClass + " does not implement AddonLifecycle"));
            }

            AddonLifecycle instance = (AddonLifecycle) mainClazz.getDeclaredConstructor().newInstance();

            loaded.put(manifest.id(), new LoadedAddon(manifest, classLoader, instance, null));

            return AddonLoadResult.success(manifest);
        } catch (Exception e) {
            return AddonLoadResult.failure(
                    null, List.of("Failed to load " + jarPath + ": " + e.getMessage()));
        }
    }

    public LoadedAddon get(String id) { return loaded.get(id); }

    public Map<String, LoadedAddon> allLoaded() { return Collections.unmodifiableMap(loaded); }

    public void unload(String id) {
        LoadedAddon la = loaded.remove(id);
        if (la != null) {
            try {
                la.classLoader().close();
            } catch (Exception e) {
                LOG.warning("Failed to close ClassLoader for addon '" + id + "': " + e.getMessage());
            }
        }
    }

    private String findMainClass(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var entry = jar.getJarEntry("addon.yml");
            if (entry == null) return null;
            // Check for a main-class property or convention
            return jar.stream()
                    .filter(e -> e.getName().endsWith(".class") && !e.getName().contains("$"))
                    .findFirst()
                    .map(e -> e.getName().replace('/', '.').replace(".class", ""))
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
