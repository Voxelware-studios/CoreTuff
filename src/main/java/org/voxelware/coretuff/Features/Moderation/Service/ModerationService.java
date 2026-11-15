package org.voxelware.coretuff.Features.Moderation.Service;

import org.voxelware.coretuff.Features.Moderation.ModIO.BanCheckResult;
import org.voxelware.coretuff.Features.Moderation.PunishmentEngine.PunishmentRecord;

import java.util.List;
import java.util.UUID;

public interface ModerationService {
	
	void ban(UUID uuid, String name, String reason, String moderator);
	
	void tempBan(UUID uuid, String name, String reason, long durationMillis, String moderator);
	
	void unban(UUID uuid, String name);
	
	BanCheckResult checkBan(UUID uuid, String name);
	BanCheckResult checkIpBan(UUID uuid, String ip);
	
	String getBanById(String id);
	PunishmentRecord getPunishmentById(String id);
	void pardonById(String id,
	                String pardonReason,
	                String moderator);
	void pardonByPlayer(UUID uuid,
	                    String name,
	                    String pardonReason,
	                    String moderator);
	void ipBan(String ip, String name, String reason, String moderator);
	void syncVanillaBans();
	void syncVanillaIpBans();
	List<PunishmentRecord> getRecentPunishments(int limit);
}
