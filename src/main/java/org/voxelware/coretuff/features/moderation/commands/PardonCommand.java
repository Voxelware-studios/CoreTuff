package org.voxelware.coretuff.features.moderation.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.voxelware.coretuff.features.moderation.modio.ModerationMessages;
import org.voxelware.coretuff.features.moderation.service.ModerationService;
import org.voxelware.coretuff.utility.CoreTuffProvider;
import org.voxelware.coretuff.commandexecuter.BaseCommand;

import java.util.Arrays;
import java.util.List;

public class PardonCommand extends BaseCommand {

	private final ModerationService moderationService;

	public PardonCommand(ModerationService moderationService) {
		this.moderationService = moderationService;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender,
	                         @NotNull Command command,
	                         @NotNull String label,
	                         @NotNull String[] args) {

		if (sender instanceof Player player && !player.hasPermission("coretuff.moderation.pardon")) {
			ModerationMessages.send(sender,
					player,
					"moderation.messages.no-permission",
					"&cYou do not have permission to use this moderation command.",
					null);
			return true;
		}

		if (args.length < 1) {
			ModerationMessages.send(sender,
					sender instanceof Player player ? player : null,
					"moderation.commands.pardon.usage",
					"&cUsage: /pardon <player> [reason]",
					null);
			return true;
		}

		String targetName = args[0];
		Player onlineTarget = Bukkit.getPlayerExact(targetName);
		String reason = args.length >= 2
				? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
				: ModerationMessages.get("moderation.commands.pardon.default-reason", "Pardoned");
		
		moderationService.pardonByPlayer(
				onlineTarget == null ? null : onlineTarget.getUniqueId(),
				targetName,
				reason,
				sender.getName()
		);

		ModerationMessages.send(sender,
				sender instanceof Player player ? player : null,
				"moderation.commands.pardon.success",
				"&aPardoned {player}",
				java.util.Map.of("player", targetName));
		broadcastUpdate();
		return true;
	}

	private void broadcastUpdate() {
		try {
			var plugin = CoreTuffProvider.getPlugin();
			if (plugin != null && plugin.getEditorModule() != null) {
				plugin.getEditorModule().broadcastModerationUpdate();
			}
		} catch (Exception ignored) {}
	}

	@Override
	protected List<String> generateCompletions(CommandSender sender, String label, String[] args) {
		if (args.length == 1) {
			return Bukkit.getOnlinePlayers().stream()
					.map(Player::getName)
					.toList();
		}
		
		if (args.length >= 2 && args[args.length - 1].isBlank()) {
			return List.of("<reason>");
		}
		
		return List.of();
	}
}
