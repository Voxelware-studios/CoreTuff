package org.voxelware.coretuff.features.moderation.listerner;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.voxelware.coretuff.features.moderation.modio.BanCheckResult;
import org.voxelware.coretuff.features.moderation.service.ModerationService;

import java.util.UUID;

public class PreLoginListener implements Listener {

	private final ModerationService moderationService;

	public PreLoginListener(ModerationService moderationService) {
		this.moderationService = moderationService;
	}

	@EventHandler
	public void onPreLogin(AsyncPlayerPreLoginEvent event) {
		String ip = event.getAddress() == null ? null : event.getAddress().getHostAddress();
		if (ip != null) {
			BanCheckResult ipResult =
					moderationService.checkIpBan(event.getUniqueId(), ip);
			if (ipResult.isBanned()) {
				event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, ipResult.getMessage());
				return;
			}
		}

		UUID uuid = event.getUniqueId();
		String name = event.getName();
		BanCheckResult result = moderationService.checkBan(uuid, name);
		if (result.isBanned()) {
			event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, result.getMessage());
		}
	}
}
