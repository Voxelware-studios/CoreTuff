package org.voxelware.coretuff.Utility;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class VersionedConfigLoader {

	private static final String VERSION_KEY = "config-version";

	private final JavaPlugin plugin;
	private final String resourcePath;
	private final File file;
	private FileConfiguration config;

	public VersionedConfigLoader(JavaPlugin plugin, String fileName, String resourcePath) {
		this.plugin = plugin;
		this.resourcePath = resourcePath;

		File coreFolder = new File(plugin.getDataFolder(), "Core");
		if (!coreFolder.exists()) {
			coreFolder.mkdirs();
		}

		this.file = new File(coreFolder, fileName);
		load();
	}

	public void reload() {
		load();
	}

	public FileConfiguration getConfig() {
		return config;
	}

	public void save() {
		try {
			config.save(file);
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}

	private void load() {
		try {
			ensureCurrentVersion();
			config = YamlConfiguration.loadConfiguration(file);
		} catch (IOException exception) {
			exception.printStackTrace();
			config = YamlConfiguration.loadConfiguration(file);
		}
	}

	private void ensureCurrentVersion() throws IOException {
		FileConfiguration bundled = loadBundledConfig();
		if (bundled == null) {
			if (!file.exists()) {
				file.createNewFile();
			}
			return;
		}

		if (!file.exists()) {
			copyBundledConfig();
			return;
		}

		FileConfiguration existing = YamlConfiguration.loadConfiguration(file);
		BigDecimal existingVersion = readVersion(existing);
		BigDecimal bundledVersion = readVersion(bundled);

		if (existingVersion.compareTo(bundledVersion) != 0) {
			renameCurrentAsOld();
			copyBundledConfig();
		}
	}

	private BigDecimal readVersion(FileConfiguration configuration) {
		Object raw = configuration.get(VERSION_KEY);
		if (raw == null) {
			return BigDecimal.valueOf(-1L);
		}

		if (raw instanceof Number number) {
			return new BigDecimal(String.valueOf(number));
		}

		String value = String.valueOf(raw).trim();
		if (value.isEmpty()) {
			return BigDecimal.valueOf(-1L);
		}

		try {
			return new BigDecimal(value);
		} catch (NumberFormatException exception) {
			return BigDecimal.valueOf(-1L);
		}
	}

	private FileConfiguration loadBundledConfig() throws IOException {
		InputStream resource = plugin.getResource(resourcePath);
		if (resource == null) {
			return null;
		}

		try (InputStream in = resource;
		     InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			return YamlConfiguration.loadConfiguration(reader);
		}
	}

	private void copyBundledConfig() throws IOException {
		InputStream resource = plugin.getResource(resourcePath);
		if (resource == null) {
			return;
		}

		try (InputStream in = resource) {
			Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void renameCurrentAsOld() throws IOException {
		String name = file.getName();
		int dot = name.lastIndexOf('.');
		String base = dot > 0 ? name.substring(0, dot) : name;
		String timestamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
		File oldFile = new File(file.getParentFile(), "old" + base + "_" + timestamp + ".yml");
		if (oldFile.exists()) oldFile.delete();
		Files.move(file.toPath(), oldFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}
}
