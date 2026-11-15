package org.voxelware.coretuff.Addons.runtime;

import org.voxelware.coretuff.Addons.api.CoreTuffAddon;
import org.voxelware.coretuff.Addons.api.CoreTuffAddonContext;
import org.voxelware.coretuff.CoreTuff;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class AddonManager {

	private static final int API_VERSION = 1;

	private final CoreTuff plugin;
	private final File addonsFolder;
	private final List<LoadedAddon> loadedAddons = new ArrayList<>();

	public AddonManager(CoreTuff plugin) {
		this.plugin = plugin;
		this.addonsFolder = new File(plugin.getDataFolder(), "Addons");
		if (!addonsFolder.exists()) {
			addonsFolder.mkdirs();
		}
	}

	public void loadAddons() {
		File[] jars = addonsFolder.listFiles(file -> file.isFile() && file.getName().toLowerCase().endsWith(".jar"));
		if (jars == null) {
			return;
		}

		for (File jarFile : jars) {
			loadAddon(jarFile);
		}
	}

	public void disableAddons() {
		for (int i = loadedAddons.size() - 1; i >= 0; i--) {
			LoadedAddon loaded = loadedAddons.get(i);
			try {
				loaded.instance().onDisable(loaded.context());
			} catch (Exception exception) {
				loaded.context().logger().warning("Failed to disable addon " + loaded.name() + ": " + exception.getMessage());
			}

			try {
				loaded.classLoader().close();
			} catch (Exception ignored) {
			}
		}

		loadedAddons.clear();
	}

	public List<String> getLoadedAddonNames() {
		return loadedAddons.stream().map(LoadedAddon::name).toList();
	}

	private void loadAddon(File jarFile) {
		try (JarFile jar = new JarFile(jarFile)) {
			JarEntry descriptorEntry = jar.getJarEntry("addon.yml");
			if (descriptorEntry == null) {
				plugin.getLogger().warning("Skipping addon " + jarFile.getName() + ": missing addon.yml");
				return;
			}

			YamlConfiguration descriptor;
			try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(descriptorEntry), StandardCharsets.UTF_8)) {
				descriptor = YamlConfiguration.loadConfiguration(reader);
			}

			String name = descriptor.getString("name");
			String main = descriptor.getString("main");
			String version = descriptor.getString("version", "unknown");
			int apiVersion = descriptor.getInt("api-version", 1);

			if (name == null || name.isBlank() || main == null || main.isBlank()) {
				plugin.getLogger().warning("Skipping addon " + jarFile.getName() + ": invalid addon.yml");
				return;
			}

			if (apiVersion != API_VERSION) {
				plugin.getLogger().warning("Skipping addon " + name + ": unsupported api-version " + apiVersion);
				return;
			}

			URLClassLoader classLoader = new URLClassLoader(
					new URL[]{jarFile.toURI().toURL()},
					CoreTuffAddon.class.getClassLoader()
			);

			Class<?> rawClass = classLoader.loadClass(main);
			if (!CoreTuffAddon.class.isAssignableFrom(rawClass)) {
				classLoader.close();
				plugin.getLogger().warning("Skipping addon " + name + ": main class does not implement CoreTuffAddon");
				return;
			}

			CoreTuffAddon addon = (CoreTuffAddon) rawClass.getDeclaredConstructor().newInstance();
			File addonDataFolder = new File(addonsFolder, name);
			if (!addonDataFolder.exists()) {
				addonDataFolder.mkdirs();
			}

			SimpleAddonContext context = new SimpleAddonContext(plugin, name, version, jarFile, addonDataFolder);
			addon.onLoad(context);
			addon.onEnable(context);
			loadedAddons.add(new LoadedAddon(name, addon, context, classLoader));
			context.logger().info("Addon enabled.");
		} catch (Exception exception) {
			plugin.getLogger().warning("Failed to load addon " + jarFile.getName() + ": " + exception.getMessage());
		}
	}

	private record LoadedAddon(
			String name,
			CoreTuffAddon instance,
			SimpleAddonContext context,
			URLClassLoader classLoader
	) {
	}

	private static final class SimpleAddonContext implements CoreTuffAddonContext {

		private final CoreTuff plugin;
		private final String addonName;
		private final String addonVersion;
		private final File addonJar;
		private final File addonDataFolder;
		private final Logger logger;

		private SimpleAddonContext(CoreTuff plugin, String addonName, String addonVersion, File addonJar, File addonDataFolder) {
			this.plugin = plugin;
			this.addonName = addonName;
			this.addonVersion = addonVersion;
			this.addonJar = addonJar;
			this.addonDataFolder = addonDataFolder;
			this.logger = Logger.getLogger("CoreTuff-Addon-" + addonName);
		}

		@Override
		public String addonName() {
			return addonName;
		}

		@Override
		public String addonVersion() {
			return addonVersion;
		}

		@Override
		public String coreVersion() {
			return plugin.getDescription().getVersion();
		}

		@Override
		public boolean isFolia() {
			return plugin.isFolia();
		}

		@Override
		public File addonDataFolder() {
			return addonDataFolder;
		}

		@Override
		public File addonJar() {
			return addonJar;
		}

		@Override
		public Logger logger() {
			return logger;
		}

		@Override
		public Map<String, Object> configSnapshot(String configName) {
			if (configName == null) {
				return Collections.emptyMap();
			}

			FileConfiguration configuration = switch (configName.toLowerCase()) {
				case "core" -> plugin.getCoreConfigLoader().getConfig();
				case "utility" -> plugin.getUtilityConfig().get();
				case "teleport" -> plugin.getTeleportConfig().get();
				case "moderation" -> plugin.getModConfig().get();
				case "security" -> plugin.getSecurityConfig().get();
				default -> null;
			};

			if (configuration == null) {
				return Collections.emptyMap();
			}

			return Collections.unmodifiableMap(new LinkedHashMap<>(configuration.getValues(true)));
		}

		@Override
		public void sendMessage(Player player, String message, Map<String, String> placeholders) {
			if (player == null) {
				return;
			}
			player.sendMessage(plugin.format(player, message, placeholders));
		}
	}
}
