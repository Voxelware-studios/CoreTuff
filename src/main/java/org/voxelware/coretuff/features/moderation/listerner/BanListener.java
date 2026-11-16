package org.voxelware.coretuff.features.moderation.listerner;


import org.voxelware.coretuff.features.moderation.service.ModerationService;

public class BanListener extends PreLoginListener {

	public BanListener(ModerationService moderationService) {
		super(moderationService);
	}
}
