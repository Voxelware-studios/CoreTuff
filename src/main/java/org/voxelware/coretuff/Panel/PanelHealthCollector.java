package org.voxelware.coretuff.Panel;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.voxelware.coretuff.CoreTuff;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a health report shaped exactly like the CoreTuff 2.0 dashboard payload so the
 * web panel can ingest it unchanged. Stats-only servers never expose this over the
 * panel API - it only feeds the public statistics aggregates.
 */
public final class PanelHealthCollector {

	private static final String[] INTEGRATIONS = {
			"PlaceholderAPI", "ProtocolLib", "Vault", "GeyserMC", "Floodgate", "packetevents", "LuckPerms"
	};

	private final CoreTuff plugin;
	private final long startTime = System.currentTimeMillis();

	public PanelHealthCollector(CoreTuff plugin) {
		this.plugin = plugin;
	}

	public Map<String, Object> collect() {
		List<Map<String, Object>> components = new ArrayList<>();
		components.add(collectServer());
		components.add(collectPlugins());
		components.add(collectIntegrations());
		components.add(collectCoreTuff());

		String overall = "HEALTHY";
		for (Map<String, Object> component : components) {
			overall = merge(overall, (String) component.get("state"));
		}

		Map<String, Object> report = new LinkedHashMap<>();
		report.put("overall_state", overall);
		report.put("timestamp", System.currentTimeMillis());
		report.put("server_uptime", ManagementFactory.getRuntimeMXBean().getUptime());
		report.put("monitor_uptime", System.currentTimeMillis() - startTime);
		report.put("components", components);
		report.put("server_info", serverInfo());
		report.put("incidents", List.of());
		return report;
	}

	private Map<String, Object> serverInfo() {
		Server server = plugin.getServer();
		Map<String, Object> info = new LinkedHashMap<>();
		info.put("plugin_version", plugin.getDescription().getVersion());
		info.put("plugin_channel", plugin.getChannel());
		info.put("server_name", server.getName());
		info.put("server_software", server.getName());
		info.put("minecraft_version", server.getMinecraftVersion());
		info.put("java_version", System.getProperty("java.version"));
		info.put("os_name", System.getProperty("os.name"));
		info.put("folia", plugin.isFolia());
		info.put("online_mode", server.getOnlineMode());
		return info;
	}

	private Map<String, Object> collectServer() {
		Server server = plugin.getServer();
		Map<String, Object> details = new LinkedHashMap<>();

		double[] tps = getTps(server);
		details.put("tps", tps);
		details.put("tps_1m", finite(tps[0], 20.0));
		details.put("tps_5m", finite(tps[1], 20.0));
		details.put("tps_15m", finite(tps[2], 20.0));

		double mspt = getMspt();
		details.put("mspt", finite(mspt, 0.0));

		MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
		long usedMb = heap.getUsed() / 1024 / 1024;
		long maxMb = heap.getMax() / 1024 / 1024;
		double memPercent = maxMb > 0 ? (usedMb * 100.0 / maxMb) : 0;
		details.put("memory_used_mb", usedMb);
		details.put("memory_max_mb", maxMb);
		details.put("memory_percent", round1(memPercent));

		double cpu = getCpuLoad();
		details.put("cpu_percent", round1(cpu));

		int onlinePlayers = server.getOnlinePlayers().size();
		details.put("online_players", onlinePlayers);
		details.put("max_players", server.getMaxPlayers());
		details.put("jvm_uptime_ms", ManagementFactory.getRuntimeMXBean().getUptime());

		String osName = System.getProperty("os.name");
		details.put("os", osName != null ? osName : "Unknown");
		details.put("arch", System.getProperty("os.arch"));
		details.put("java_version", System.getProperty("java.version"));
		details.put("java_vendor", System.getProperty("java.vendor"));
		details.put("worlds", server.getWorlds().size());

		details.put("server_software", server.getName());
		details.put("server_version", Bukkit.getVersion());
		details.put("minecraft_version", server.getMinecraftVersion());
		details.put("bukkit_version", Bukkit.getBukkitVersion());
		details.put("folia", plugin.isFolia());
		details.put("online_mode", server.getOnlineMode());

		String state = "HEALTHY";
		String message = "Server running normally";
		if (tps[0] < 18.0) {
			state = "WARNING";
			message = "Low TPS (" + String.format(java.util.Locale.ROOT, "%.1f", tps[0]) + ")";
		}
		if (mspt > 100) {
			state = "CRITICAL";
			message = "Very high MSPT (" + String.format(java.util.Locale.ROOT, "%.1f", mspt) + "ms)";
		} else if (mspt > 50 && "HEALTHY".equals(state)) {
			state = "WARNING";
			message = "Elevated MSPT (" + String.format(java.util.Locale.ROOT, "%.1f", mspt) + "ms)";
		}
		details.put("performance_status", state);

		return component("Server", state, message, details);
	}

	private Map<String, Object> collectPlugins() {
		Map<String, Object> details = new LinkedHashMap<>();
		int total = 0;
		int loaded = 0;
		int failed = 0;
		List<Map<String, Object>> plugins = new ArrayList<>();

		for (var pluginDescriptor : Bukkit.getPluginManager().getPlugins()) {
			total++;
			boolean enabled = pluginDescriptor.isEnabled();
			if (enabled) {
				loaded++;
			} else {
				failed++;
			}
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("name", pluginDescriptor.getName());
			entry.put("version", pluginDescriptor.getDescription().getVersion());
			entry.put("enabled", enabled);
			plugins.add(entry);
		}

		details.put("total_plugins", total);
		details.put("loaded_plugins", loaded);
		details.put("failed_plugins", failed);
		details.put("plugins", plugins);

		String state = failed > 0 ? "WARNING" : "HEALTHY";
		String message = failed > 0
				? failed + " plugin(s) failed to enable"
				: loaded + "/" + total + " plugins loaded";
		return component("Plugins", state, message, details);
	}

	private Map<String, Object> collectIntegrations() {
		Map<String, Object> details = new LinkedHashMap<>();
		List<Map<String, Object>> integrations = new ArrayList<>();
		int active = 0;

		for (String name : INTEGRATIONS) {
			boolean present = Bukkit.getPluginManager().getPlugin(name) != null;
			if (present) {
				active++;
			}
			Map<String, Object> integration = new LinkedHashMap<>();
			integration.put("name", name);
			integration.put("status", present ? "ACTIVE" : "INACTIVE");
			integrations.add(integration);
		}

		details.put("integrations", integrations);
		String message = active + "/" + INTEGRATIONS.length + " integrations active";
		return component("Integrations", "HEALTHY", message, details);
	}

	private Map<String, Object> collectCoreTuff() {
		List<Map<String, Object>> modules = new ArrayList<>();
		addModule(modules, "Economy", plugin.economyService() != null);
		addModule(modules, "Moderation", plugin.moderationService() != null);
		addModule(modules, "Warps", plugin.warpService() != null);
		addModule(modules, "Jails", plugin.jailService() != null);
		addModule(modules, "Kits", plugin.kitService() != null);

		List<String> activeModules = new ArrayList<>();
		for (Map<String, Object> module : modules) {
			if (Boolean.TRUE.equals(module.get("active"))) {
				activeModules.add((String) module.get("name"));
			}
		}

		Map<String, Object> details = new LinkedHashMap<>();
		details.put("version", plugin.getDescription().getVersion());
		details.put("channel", plugin.getChannel());
		details.put("active_modules", activeModules);
		details.put("total_modules", modules.size());
		details.put("active_module_count", activeModules.size());

		Map<String, Object> modulesGroup = new LinkedHashMap<>();
		modulesGroup.put("name", "Modules");
		modulesGroup.put("total", modules.size());
		modulesGroup.put("active", activeModules.size());
		modulesGroup.put("module_list", modules);
		if (activeModules.isEmpty()) {
			modulesGroup.put("state", "CRITICAL");
			modulesGroup.put("message", "No modules active");
		} else if (activeModules.size() == modules.size()) {
			modulesGroup.put("state", "HEALTHY");
			modulesGroup.put("message", activeModules.size() + "/" + modules.size() + " modules active");
		} else {
			modulesGroup.put("state", "WARNING");
			modulesGroup.put("message", activeModules.size() + "/" + modules.size() + " modules active");
		}
		details.put("subsystems", List.of(modulesGroup));

		String state = activeModules.isEmpty() ? "CRITICAL" : (activeModules.size() == modules.size() ? "HEALTHY" : "WARNING");
		String message = state.equals("HEALTHY")
				? "All CoreTuff modules operational"
				: activeModules.size() + "/" + modules.size() + " modules active";
		return component("CoreTuff", state, message, details);
	}

	private static void addModule(List<Map<String, Object>> modules, String name, boolean active) {
		Map<String, Object> module = new LinkedHashMap<>();
		module.put("name", name);
		module.put("active", active);
		modules.add(module);
	}

	private static Map<String, Object> component(String name, String state, String message, Map<String, Object> details) {
		Map<String, Object> component = new LinkedHashMap<>();
		component.put("name", name);
		component.put("state", state);
		component.put("message", message);
		component.put("details", details);
		return component;
	}

	private static String merge(String current, String next) {
		if ("CRITICAL".equals(current) || "CRITICAL".equals(next)) return "CRITICAL";
		if ("WARNING".equals(current) || "WARNING".equals(next)) return "WARNING";
		if ("UNKNOWN".equals(current) || "UNKNOWN".equals(next)) return "UNKNOWN";
		return "HEALTHY";
	}

	private double[] getTps(Server server) {
		try {
			double[] tps = server.getTPS();
			if (tps != null && tps.length >= 3) {
				return new double[]{finite(tps[0], 20.0), finite(tps[1], 20.0), finite(tps[2], 20.0)};
			}
		} catch (Throwable ignored) {
		}
		return new double[]{20.0, 20.0, 20.0};
	}

	private double getMspt() {
		try {
			Object result = plugin.getServer().getClass().getMethod("getAverageTickTime").invoke(plugin.getServer());
			return result instanceof Double d ? d : 0.0;
		} catch (Throwable ignored) {
			return 0.0;
		}
	}

	private double getCpuLoad() {
		try {
			var osBean = ManagementFactory.getOperatingSystemMXBean();
			if (osBean instanceof com.sun.management.OperatingSystemMXBean sunBean) {
				return sunBean.getProcessCpuLoad() * 100;
			}
		} catch (Throwable ignored) {
		}
		return -1.0;
	}

	private static double finite(double value, double fallback) {
		return Double.isFinite(value) ? value : fallback;
	}

	private static double round1(double value) {
		return Math.round(value * 10.0) / 10.0;
	}
}
