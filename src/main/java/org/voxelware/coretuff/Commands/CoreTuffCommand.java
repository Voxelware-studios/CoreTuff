package org.voxelware.coretuff.Commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.voxelware.coretuff.CoreTuff;
import org.voxelware.coretuff.Features.Database.DumpService;
import org.voxelware.coretuff.Utility.ComponentUtils;
import org.voxelware.coretuff.Utility.Diagnostic.DiagnosticDumpService;
import org.voxelware.coretuff.commandExecuter.BaseCommand;
import org.voxelware.coretuff.dashboard.DashboardModule;
import org.voxelware.coretuff.editor.EditorModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CoreTuffCommand extends BaseCommand {

	private final CoreTuff plugin;

	public CoreTuffCommand(CoreTuff plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(
			@NotNull CommandSender sender,
			@NotNull Command command,
			@NotNull String label,
			@NotNull String[] args
	) {
		if (args.length == 0) {
			sender.sendMessage(createErrorComponent("Usage: /%s <reload|dump|dumpdata|pluginHide|dash|dashboard|bug" + (plugin.isDevMode() ? "|testincident" : "") + ">", label));
			return true;
		}

		switch (args[0].toLowerCase()) {
			case "reload" -> {
				if (!sender.hasPermission("coretuff.reload")) {
					sender.sendMessage(plugin.format(null, plugin.getCoreString("nopermission"), null));
					return true;
				}

				plugin.reloadPluginState();
				sender.sendMessage(createSuccessComponent("CoreTuff reloaded."));
				return true;
			}
			case "dump" -> {
				if (!sender.hasPermission("coretuff.admin.dump")) {
					sender.sendMessage(plugin.format(null, plugin.getCoreString("nopermission"), null));
					return true;
				}

				new Thread(() -> {
					DiagnosticDumpService ds = plugin.diagnosticDumpService();
					if (ds == null) {
						sender.sendMessage(createErrorComponent("Diagnostic system not initialized."));
						return;
					}
					DiagnosticDumpService.DiagnosticResult result = ds.generate();
					if (result.success()) {
						sender.sendMessage(createSuccessComponent("CoreTuff diagnostic dump generated successfully."));
						sender.sendMessage(createSuccessComponent("Location:"));
						sender.sendMessage(ComponentUtils.translateLegacyText("  &f" + result.message()));
					} else {
						sender.sendMessage(createErrorComponent("Failed to generate diagnostic dump: " + result.message()));
					}
				}, "CoreTuff-DiagnosticDump").start();
				return true;
			}
			case "dumpdata" -> {
				if (!sender.hasPermission("coretuff.dump")) {
					sender.sendMessage(plugin.format(null, plugin.getCoreString("nopermission"), null));
					return true;
				}

				DumpService dumpService = new DumpService(plugin);
				DumpService.DumpResult result = dumpService.dumpAll();
				if (result.success()) {
					sender.sendMessage(createSuccessComponent(result.message()));
				} else {
					sender.sendMessage(createErrorComponent(result.message()));
				}
				return true;
			}
			case "pluginhide" -> {
				if (!sender.hasPermission("coretuff.plhide")) {
					sender.sendMessage(plugin.format(null, plugin.getCoreString("nopermission"), null));
					return true;
				}

				if (args.length < 2) {
					boolean current = plugin.getSecurityConfig().pluginHideEnabled();
					sender.sendMessage(createSuccessComponent("Plugin hiding is currently " + current));
					sender.sendMessage(createErrorComponent("Usage: /%s pluginHide <true/false>", label));
					return true;
				}

				boolean value = Boolean.parseBoolean(args[1]);
				plugin.getSecurityConfig().setPluginHide(value);
				sender.sendMessage(createSuccessComponent("Plugin hiding set to " + value));
				return true;
			}
			case "dash" -> {
				if (!sender.hasPermission("coretuff.admin") && !sender.isOp()) {
					sender.sendMessage(plugin.format(null, plugin.getCoreString("nopermission"), null));
					return true;
				}

				EditorModule em = plugin.getEditorModule();
				if (em == null) {
					sender.sendMessage(createErrorComponent("Editor module not available."));
					return true;
				}

				if (!em.isRunning()) {
					try {
						em.start();
					} catch (Exception e) {
						sender.sendMessage(createErrorComponent("Failed to start editor module: " + e.getMessage()));
						return true;
					}
				}

				try {
					java.security.SecureRandom random = new java.security.SecureRandom();
					byte[] sessionBytes = new byte[8];
					random.nextBytes(sessionBytes);
					java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(sessionBytes);
					long numericId = buf.getLong();
					String sessionId = Long.toUnsignedString(numericId, 36);

					var registry = plugin.getSettingsRegistry();
					var schema = registry.getSchema();
					var values = registry.getSettings();
					String serverName = plugin.getServer().getName();
					String pluginVersion = plugin.getPluginMeta().getVersion();

					com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
					payload.addProperty("session_id", sessionId.toString());
					payload.addProperty("server_name", serverName);
					payload.addProperty("plugin_version", pluginVersion);
					payload.addProperty("server_id", em.getConfig().getServerId());
					payload.addProperty("expires_in_minutes", em.getConfig().getSessionExpiryMinutes());

					com.google.gson.JsonObject schemaJson = new com.google.gson.JsonObject();
					for (var schemaEntry : schema.entrySet()) {
						com.google.gson.JsonArray defs = new com.google.gson.JsonArray();
						for (var def : schemaEntry.getValue()) {
							com.google.gson.JsonObject d = new com.google.gson.JsonObject();
							d.addProperty("key", def.key());
							d.addProperty("type", def.type().getClass().getSimpleName().replace("Type", "").toLowerCase());
							if (def.defaultValue() != null) d.addProperty("default", def.defaultValue().toString());
							if (def.description() != null) d.addProperty("description", def.description());
							if (def.category() != null) d.addProperty("category", def.category());
							if (def.min() != null) d.addProperty("min", def.min());
							if (def.max() != null) d.addProperty("max", def.max());
							if (def.options() != null && !def.options().isEmpty()) {
								com.google.gson.JsonArray opts = new com.google.gson.JsonArray();
								for (String o : def.options()) opts.add(o);
								d.add("options", opts);
							}
							defs.add(d);
						}
						schemaJson.add(schemaEntry.getKey(), defs);
					}
					payload.add("schema", schemaJson);

					com.google.gson.JsonObject valuesJson = new com.google.gson.JsonObject();
					for (var valEntry : values.entrySet()) {
						com.google.gson.JsonObject moduleVals = new com.google.gson.JsonObject();
						for (var v : valEntry.getValue().entrySet()) {
							moduleVals.add(v.getKey(), toJsonElement(v.getValue()));
						}
						valuesJson.add(valEntry.getKey(), moduleVals);
					}
					payload.add("values", valuesJson);

					String backendUrl = em.getConfig().getBackendUrl();
					java.net.URL url = new java.net.URL(backendUrl + "/api/editor/create");
					java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
					conn.setRequestMethod("POST");
					conn.setRequestProperty("Content-Type", "application/json");
					conn.setDoOutput(true);
					conn.setConnectTimeout(5000);
					conn.setReadTimeout(5000);
					try (java.io.OutputStream os = conn.getOutputStream()) {
						os.write(new com.google.gson.Gson().toJson(payload).getBytes(java.nio.charset.StandardCharsets.UTF_8));
					}
					int responseCode = conn.getResponseCode();
					conn.disconnect();

					if (responseCode == 201) {
						String dashUrl = em.getConfig().getFrontendUrl() + "/d/" + sessionId.toString();
						plugin.getLogger().info("[CoreTuff] Dashboard session created.");
						sender.sendMessage(ComponentUtils.translateLegacyText("&a&lCoreTuff Dashboard"));
						sender.sendMessage(ComponentUtils.translateLegacyText("&7Open this link:"));
						sender.sendMessage(
								Component.text(dashUrl, NamedTextColor.AQUA)
										.clickEvent(ClickEvent.openUrl(dashUrl))
										.hoverEvent(HoverEvent.showText(Component.text("Click to open")))
						);

						com.google.gson.JsonObject sessionCreated = new com.google.gson.JsonObject();
						sessionCreated.addProperty("type", "session_created");
						sessionCreated.addProperty("session_id", sessionId);
						sessionCreated.addProperty("server_id", em.getConfig().getServerId());
						em.sendMessage(sessionCreated);

						em.activateSession(sessionId);
					} else {
						sender.sendMessage(createErrorComponent("Failed to create dashboard session. Backend returned " + responseCode));
					}
				} catch (java.net.ConnectException e) {
					sender.sendMessage(createErrorComponent("Cannot connect to editor backend at " + em.getConfig().getBackendUrl() + ". Is it running?"));
				} catch (Exception e) {
					sender.sendMessage(createErrorComponent("Failed to create dashboard session: " + e.getMessage()));
					plugin.getLogger().warning("Dashboard session creation failed: " + e.getMessage());
				}
				return true;
			}
			case "dashboard" -> {
				if (!sender.hasPermission("coretuff.admin") && !sender.isOp()) {
					sender.sendMessage(plugin.format(null, plugin.getCoreString("nopermission"), null));
					return true;
				}

				DashboardModule dm = plugin.getDashboardModule();
				if (dm == null) {
					sender.sendMessage(createErrorComponent("Dashboard module not available."));
					return true;
				}

				if (args.length < 2) {
					sender.sendMessage(createSuccessComponent("Dashboard: " + (dm.isRunning() ? "Running on port " + dm.getPort() : "Stopped")));
					sender.sendMessage(createErrorComponent("Usage: /%s dashboard <pair|status|restart>", label));
					return true;
				}

				switch (args[1].toLowerCase()) {
					case "pair" -> {
						if (!dm.isRunning()) {
							sender.sendMessage(createErrorComponent("Dashboard module is not running."));
							return true;
						}
						String ip = sender instanceof Player p ? p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : "unknown" : "console";
						String serverName = dm.getPlugin().getServer().getName();
						String serverUuid = java.util.UUID.nameUUIDFromBytes(serverName.getBytes()).toString();
						String code = dm.getPairingService().generateCode(ip, serverName, serverUuid);
						if (code == null) {
							sender.sendMessage(createErrorComponent("Failed to generate pair code. Maximum sessions reached."));
							return true;
						}
						int expiresIn = dm.getConfig().getPairCodeExpiryMinutes();

						sender.sendMessage(ComponentUtils.translateLegacyText("&a&lCoreTuff Dashboard Pairing"));
						sender.sendMessage(ComponentUtils.translateLegacyText("&7Your pair code:"));
						sender.sendMessage(
								Component.text("  &b&l" + code, NamedTextColor.AQUA)
										.clickEvent(ClickEvent.copyToClipboard(code))
										.hoverEvent(HoverEvent.showText(Component.text("Click to copy")))
						);
						sender.sendMessage(ComponentUtils.translateLegacyText("&7Expires in " + expiresIn + " minutes"));
						sender.sendMessage(ComponentUtils.translateLegacyText("&7Enter this code in the CoreTuff Dashboard to connect."));
					}
					case "status" -> {
						if (!dm.isRunning()) {
							sender.sendMessage(createErrorComponent("Dashboard module is not running."));
							return true;
						}
						int activeSessions = dm.getPairingService().getActiveSessionCount();
						sender.sendMessage(createSuccessComponent("Dashboard Status:"));
						sender.sendMessage(createSuccessComponent("  Running: true"));
						sender.sendMessage(createSuccessComponent("  Port: " + dm.getPort()));
						sender.sendMessage(createSuccessComponent("  Active Pair Sessions: " + activeSessions));
						sender.sendMessage(createSuccessComponent("  WebSocket Clients: " + dm.getWebSocketManager().getConnectedClients()));
					}
					case "restart" -> {
						dm.stop();
						dm.start();
						sender.sendMessage(createSuccessComponent("Dashboard module restarted."));
					}
					default -> {
						sender.sendMessage(createErrorComponent("Unknown dashboard subcommand."));
					}
				}
				return true;
			}
			case "bug" ->{
				if (sender.isOp()){
					sender.sendMessage(

							ComponentUtils.translateLegacyText(
									"&f&l[&b&lCoreTuff&f&l]:&r "
											+ "If you encounter any bugs you can report here: "
							).append(

									Component.text(
													"GitHub Issues",
													NamedTextColor.AQUA
											)
											.clickEvent(
													ClickEvent.openUrl(
															"https://github.com/Voxelware-studios/CoreTuff/issues"
													)
											)
											.hoverEvent(
													HoverEvent.showText(
															Component.text(
																	"Click to open GitHub Issues"
															)
													)
											)
							)
					);
				}
				return true;
			}
			case "testincident" -> {
				if (!plugin.isDevMode()) {
					sender.sendMessage(createErrorComponent("This command is only available in dev mode."));
					return true;
				}
				if (!sender.hasPermission("coretuff.admin") && !sender.isOp()) {
					sender.sendMessage(plugin.format(null, plugin.getCoreString("nopermission"), null));
					return true;
				}

				EditorModule em = plugin.getEditorModule();
				if (em == null || em.getHealthMonitor() == null) {
					sender.sendMessage(createErrorComponent("Health monitor not available."));
					return true;
				}

				var incidentManager = em.getHealthMonitor().getIncidentManager();
				String subCommand = args.length > 1 ? args[1].toLowerCase() : "warning";

				switch (subCommand) {
					case "warning" -> {
						var existing = incidentManager.getActiveIncidents().stream()
								.filter(i -> i.getType() == org.voxelware.coretuff.dashboard.health.IncidentType.PERFORMANCE)
								.findFirst();
						if (existing.isPresent()) {
							sender.sendMessage(createErrorComponent("Active performance incident already exists: " + existing.get().getId() + ". Use 'critical' to escalate or 'resolve' to resolve."));
							return true;
						}
						var incident = incidentManager.createIncident(
								org.voxelware.coretuff.dashboard.health.IncidentType.PERFORMANCE,
								"Test performance warning",
								org.voxelware.coretuff.dashboard.health.IncidentSeverity.WARNING,
								"Dev-mode test incident: WARNING severity"
						);
						incident.addSymptom("tps", "18.5");
						incident.addSymptom("mspt", "45ms");
						incident.addSymptom("memory", "75%");
						incident.updateValue("tps", 18.5);
						incident.updateValue("mspt", 45.0);
						incident.updateValue("memory_percent", 75.0);
						sender.sendMessage(createSuccessComponent("Test incident created: " + incident.getId() + " [WARNING]"));
					}
					case "critical" -> {
						var active = incidentManager.getActiveIncidents().stream()
								.filter(i -> i.getType() == org.voxelware.coretuff.dashboard.health.IncidentType.PERFORMANCE)
								.findFirst();
						if (active.isEmpty()) {
							sender.sendMessage(createErrorComponent("No active performance incident. Run 'testincident warning' first."));
							return true;
						}
						var incident = active.get();
						incidentManager.escalateIncident(incident,
								org.voxelware.coretuff.dashboard.health.IncidentSeverity.CRITICAL,
								"Dev-mode escalation to CRITICAL");
						incident.addSymptom("tps", "14.2");
						incident.addSymptom("mspt", "120ms");
						incident.addSymptom("memory", "95%");
						incident.updateValue("tps", 14.2);
						incident.updateValue("mspt", 120.0);
						incident.updateValue("memory_percent", 95.0);
						sender.sendMessage(createSuccessComponent("Incident escalated: " + incident.getId() + " [WARNING -> CRITICAL]"));
					}
					case "resolve" -> {
						var active = incidentManager.getActiveIncidents();
						if (active.isEmpty()) {
							sender.sendMessage(createErrorComponent("No active incidents to resolve."));
							return true;
						}
						incidentManager.resolveIncident(active.getFirst(), "Dev-mode manual resolve");
						sender.sendMessage(createSuccessComponent("Resolved incident: " + active.getFirst().getId()));
					}
					default -> {
						sender.sendMessage(createErrorComponent("Usage: /%s testincident [warning|critical|resolve]", label));
					}
				}
				return true;
			}
			default -> {
				sender.sendMessage(createErrorComponent("Unknown subcommand."));
				return true;
			}
		}
	}

	@Override
	protected List<String> generateCompletions(CommandSender sender, String label, String[] args) {
		if (args.length == 1) {
			List<String> completions = new ArrayList<>();
			if (sender.hasPermission("coretuff.reload")) {
				completions.add("reload");
			}
			if (sender.hasPermission("coretuff.plhide")) {
				completions.add("pluginHide");
			}
			if (sender.hasPermission("coretuff.admin.dump")) {
				completions.add("dump");
			}
			if (sender.hasPermission("coretuff.dump")) {
				completions.add("dumpdata");
			}
			if (sender.hasPermission("coretuff.admin") || sender.isOp()) {
				completions.add("dashboard");
				completions.add("dash");
			}
			if(sender.isOp())
			{
				completions.add("bug");
			}
			if (plugin.isDevMode() && (sender.hasPermission("coretuff.admin") || sender.isOp())) {
				completions.add("testincident");
			}
			return completions;
		}

		if (args.length == 2 && args[0].equalsIgnoreCase("dashboard") && (sender.hasPermission("coretuff.admin") || sender.isOp())) {
			return List.of("pair", "status", "restart");
		}

		if (args.length == 2 && args[0].equalsIgnoreCase("pluginHide") && sender.hasPermission("coretuff.plhide")) {
			return List.of("true", "false");
		}

		if (args.length == 2 && args[0].equalsIgnoreCase("testincident") && plugin.isDevMode()) {
			return List.of("warning", "critical", "resolve");
		}

		return List.of();
	}

	private com.google.gson.JsonElement toJsonElement(Object val) {
		if (val == null) return com.google.gson.JsonNull.INSTANCE;
		if (val instanceof String s) return new com.google.gson.JsonPrimitive(s);
		if (val instanceof Number n) return new com.google.gson.JsonPrimitive(n);
		if (val instanceof Boolean b) return new com.google.gson.JsonPrimitive(b);
		if (val instanceof java.util.List<?> list) {
			com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
			for (var item : list) arr.add(toJsonElement(item));
			return arr;
		}
		if (val instanceof java.util.Map<?, ?> map) {
			com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
			for (var entry : map.entrySet()) {
				obj.add(String.valueOf(entry.getKey()), toJsonElement(entry.getValue()));
			}
			return obj;
		}
		return new com.google.gson.JsonPrimitive(val.toString());
	}
}
