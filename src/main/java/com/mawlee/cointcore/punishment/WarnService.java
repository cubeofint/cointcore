package com.mawlee.cointcore.punishment;

import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class WarnService {
    private WarnService() {
    }

    public static boolean warn(CommandSourceStack source, UUID targetId, String targetName, String reason) {
        String issuer = source.getTextName();
        PunishmentHistory.record(
                source.getServer(),
                PunishmentType.WARN,
                targetId,
                targetName,
                issuer,
                reason,
                PunishmentHistory.NO_EXPIRY
        );

        source.sendSuccess(
                () -> CointCoreMessages.forSource(source, CointCoreMessages.WARN_APPLIED, issuer, targetName),
                true
        );

        ServerPlayer target = source.getServer().getPlayerList().getPlayer(targetId);
        if (target != null) {
            target.sendSystemMessage(CointCoreMessages.forPlayer(target, CointCoreMessages.WARN_RECEIVED, issuer, reason));
        }

        return true;
    }
}
