package com.mawlee.cointcore.ban;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.mute.MuteService;
import com.mawlee.cointcore.mute.TimeUtil;
import com.mawlee.cointcore.punishment.PunishmentHistory;
import com.mawlee.cointcore.punishment.PunishmentType;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;

import java.util.Date;
import java.util.UUID;

public final class BanService {
    private BanService() {
    }

    public static boolean ban(CommandSourceStack source, UUID targetId, String targetName, String durationToken, String reason) throws CommandSyntaxException {
        long durationMs = MuteService.parseDurationMs(durationToken);
        String banner = source.getTextName();
        GameProfile profile = new GameProfile(targetId, targetName);
        UserBanList bans = source.getServer().getPlayerList().getBans();

        if (bans.get(profile) != null) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.BAN_ALREADY_BANNED, targetName));
            return false;
        }

        Date expires = durationMs <= 0L ? null : new Date(System.currentTimeMillis() + durationMs);
        UserBanListEntry entry = new UserBanListEntry(profile, null, banner, expires, reason);
        bans.add(entry);

        long expiresAt = durationMs <= 0L ? PunishmentHistory.PERMANENT : System.currentTimeMillis() + durationMs;
        PunishmentHistory.record(
                source.getServer(),
                PunishmentType.BAN,
                targetId,
                targetName,
                banner,
                reason,
                expiresAt
        );

        source.sendSuccess(
                () -> durationMs <= 0L
                        ? CointCoreMessages.forSource(source, CointCoreMessages.BAN_APPLIED_PERM, banner, targetName)
                        : CointCoreMessages.forSource(source, CointCoreMessages.BAN_APPLIED, banner, targetName, TimeUtil.formatDuration(durationMs)),
                true
        );

        ServerPlayer target = source.getServer().getPlayerList().getPlayer(targetId);
        if (target != null) {
            target.connection.disconnect(buildKickMessage(target, banner, durationMs, reason));
        }

        return true;
    }

    private static Component buildKickMessage(ServerPlayer target, String banner, long durationMs, String reason) {
        if (durationMs <= 0L) {
            return CointCoreMessages.forPlayer(target, CointCoreMessages.BAN_RECEIVED_PERM, banner, reason);
        }

        return CointCoreMessages.forPlayer(
                target,
                CointCoreMessages.BAN_RECEIVED,
                banner,
                TimeUtil.formatDuration(durationMs),
                reason
        );
    }
}
