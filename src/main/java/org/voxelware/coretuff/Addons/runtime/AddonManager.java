package org.voxelware.coretuff.Addons.runtime;

import org.voxelware.coretuff.Addons.api.CoreTuffAddon;
import org.voxelware.coretuff.Addons.api.CoreTuffAddonContext;
import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.api.addon.AddonLifecycle;
import org.voxelware.coretuff.api.addon.AddonLoadResult;
import org.voxelware.coretuff.internal.platform.PlatformImpl;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class AddonManager {

	private static final int API_VERSION = 1;

	private final CoreTuff plugin;
	private final PlatformImpl platform;
	private final File addonsFolder;
	private final List<OldLoadedAddon> oldAddons = new ArrayList<>();

	public AddonManager(CoreTuff plugin, PlatformImpl platform) {
		this.plugin = plugin;
		this.platform = platform;
		this.addonsFolder = new File(plugin.getDataFolder(), "addons");
		if (!addonsFolder.exists()) {
			addonsFolder.mkdirs();
		}
	}

	public void loadAddons() {
		File[] jars = addonsFolder.listFiles(file -> file.isFile() && file.getName().toLowerCase().endsWith(".jar"));
		if (jars == null) return;

		List<File> oldStyle = new ArrayList<>();
		List<File> newStyle = new ArrayList<>();

		for (File jarFile : jars) {
			if (isNewStyleAddon(jarFile)) {
				newStyle.add(jarFile);
			} else {
				oldStyle.add(jarFile);
			}
		}

		if (!newStyle.isEmpty()) {
			List<AddonLoadResult> results = platform.loadAllAddons(addonsFolder.toPath());
			for (AddonLoadResult r : results) {
				if (r.success()) {
					plugin.getLogger().info("Loaded addon: " + r.manifest().name() + " v" + r.manifest().version());
				} else {
					plugin.getLogger().warning("Failed to load addon: " + String.join("; ", r.errors()));
				}
				if (!r.warnings().isEmpty()) {
					for (String w : r.warnings()) {
						plugin.getLogger().warning("Addon warning: " + w);
					}
				}
			}
		}

		for (File jarFile : oldStyle) {
			loadLegacyAddon(jarFile);
		}
	}

	public void disableAddons() {
		platform.shutdown();
		for (int i = oldAddons.size() - 1; i >= 0; i--) {
			OldLoadedAddon loaded = oldAddons.get(i);
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
		oldAddons.clear();
	}

	public List<String> getLoadedAddonNames() {
		List<String> names = new ArrayList<>();
		for (OldLoadedAddon a : oldAddons) names.add(a.name());
		names.addAll(platform.getAllAddonIds());
		return names;
	}

	private boolean isNewStyleAddon(File jarFile) {
		try (JarFile jar = new JarFile(jarFile)) {
			JarEntry entry = jar.getJarEntry("addon.yml");
			if (entry == null) return false;
			try (InputStreamReader reader = new InputStreamReader(jar.getInputStream(entry), StandardCharsets.UTF_8)) {
				YamlConfiguration descriptor = YamlConfiguration.loadConfiguration(reader);
				return !descriptor.contains("main");
			}
		} catch (Exception e) {
			return false;
		}
	}

	private void loadLegacyAddon(File jarFile) {
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
			oldAddons.add(new OldLoadedAddon(name, addon, context, classLoader));
			context.logger().info("Addon enabled.");
		} catch (Exception exception) {
			plugin.getLogger().warning("Failed to load addon " + jarFile.getName() + ": " + exception.getMessage());
		}
	}

	private record OldLoadedAddon(
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

			var cfgService = plugin.getConfigurationService();
			if (cfgService == null) return Collections.emptyMap();

			Map<String, Object> result = new LinkedHashMap<>();
			String module = switch (configName.toLowerCase()) {
				case "core" -> "core";
				case "utility" -> "utility";
				case "teleport" -> "teleport";
				case "moderation" -> "moderation";
				case "security" -> "security";
				default -> null;
			};
			if (module == null) return Collections.emptyMap();

			for (var entry : cfgService.getModule(module).entrySet()) {
				result.put(entry.getKey(), entry.getValue());
			}
			return Collections.unmodifiableMap(result);
		}

		@Override
		public void sendMessage(Player player, String message, Map<String, String> placeholders) {
			if (player == null) {
				return;
			}
			player.sendMessage(plugin.format(player, message, placeholders));
		}

		@Override
		public org.voxelware.coretuff.Features.Teleportation.Handler.ExecutionHandler executionHandler() {
			return org.voxelware.coretuff.Utility.CoreTuffProvider.getExecutionHandler();
		}

		@Override
		public org.voxelware.coretuff.Features.Teleportation.Handler.TeleportHandler teleportHandler() {
			return org.voxelware.coretuff.Utility.CoreTuffProvider.getTeleportHandler();
		}

		@Override
		public org.voxelware.coretuff.Features.Teleportation.DelayedTeleporter delayedTeleporter() {
			return org.voxelware.coretuff.Utility.CoreTuffProvider.getDelayedTeleporter();
		}

		@Override
		public org.voxelware.coretuff.Features.Teleportation.TpManager tpManager() {
			return org.voxelware.coretuff.Utility.CoreTuffProvider.getTpManager();
		}

		@Override
		public org.voxelware.coretuff.api.economy.EconomyApi getEconomyApi() {
			return plugin.getEconomyApi();
		}

		@Override
		public org.voxelware.coretuff.api.economy.TransactionApi getTransactionApi() {
			return plugin.getTransactionApi();
		}

		@Override
		public org.voxelware.coretuff.api.economy.InterestApi getInterestApi() {
			return plugin.getInterestApi();
		}
	}
}
