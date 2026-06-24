package com.mawlee.cointcore.mute;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.punishment.PunishmentHistory;
import com.mawlee.cointcore.punishment.PunishmentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class MuteService {
    private static final SimpleCommandExceptionType INVALID_DURATION = new SimpleCommandExceptionType(
            Component.literal("Invalid duration. Use: 10s, 5m, 2h, 1d, perm")
    );

    private MuteService() {
    }

    public static Optional<UUID> resolvePlayerId(MinecraftServer server, String name) {
        ServerPlayer onlineMatch = null;

        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (!online.getGameProfile().getName().equalsIgnoreCase(name)) {
                continue;
            }

            if (onlineMatch != null) {
                return Optional.empty();
            }

            onlineMatch = online;
        }

        if (onlineMatch != null) {
            return Optional.of(onlineMatch.getUUID());
        }

        return server.getProfileCache()
                .get(name)
                .map(profile -> profile.getId());
    }

    public static Optional<String> resolveName(MinecraftServer server, UUID playerId) {
        ServerPlayer online = server.getPlayerList().getPlayer(playerId);
        if (online != null) {
            return Optional.of(online.getGameProfile().getName());
        }

        return server.getProfileCache()
                .get(playerId)
                .map(profile -> profile.getName());
    }

    public static boolean mute(CommandSourceStack source, UUID targetId, String targetName, String durationToken, String reason) throws CommandSyntaxException {
        long durationMs = parseDurationMs(durationToken);
        String muter = source.getTextName();
        long expiresAt = durationMs <= 0L ? 0L : System.currentTimeMillis() + durationMs;
        MuteInfo info = new MuteInfo(muter, reason, expiresAt);

        MuteManager.apply(source.getServer(), targetId, info);

        PunishmentHistory.record(
                source.getServer(),
                PunishmentType.MUTE,
                targetId,
                targetName,
                muter,
                reason,
                expiresAt
        );

        source.sendSuccess(
                () -> durationMs <= 0L
                        ? CointCoreMessages.forSource(source, CointCoreMessages.MUTE_APPLIED_PERM, muter, targetName)
                        : CointCoreMessages.forSource(source, CointCoreMessages.MUTE_APPLIED, muter, targetName, TimeUtil.formatDuration(durationMs)),
                true
        );

        ServerPlayer target = source.getServer().getPlayerList().getPlayer(targetId);
        if (target != null) {
            target.sendSystemMessage(durationMs <= 0L
                    ? CointCoreMessages.forPlayer(target, CointCoreMessages.MUTE_RECEIVED_PERM, muter, reason)
                    : CointCoreMessages.forPlayer(
                            target,
                            CointCoreMessages.MUTE_RECEIVED,
                            muter,
                            TimeUtil.formatDuration(durationMs),
                            reason
                    ));
        }

        return true;
    }

    public static boolean unmute(CommandSourceStack source, UUID targetId, String targetName) {
        if (!MuteManager.isMuted(source.getServer(), targetId)) {
            source.sendFailure(CointCoreMessages.forSource(source, CointCoreMessages.MUTE_NOT_MUTED, targetName));
            return false;
        }

        MuteManager.remove(source.getServer(), targetId);

        PunishmentHistory.record(
                source.getServer(),
                PunishmentType.UNMUTE,
                targetId,
                targetName,
                source.getTextName(),
                "",
                PunishmentHistory.NO_EXPIRY
        );

        source.sendSuccess(
                () -> CointCoreMessages.forSource(source, CointCoreMessages.UNMUTE_APPLIED, targetName),
                true
        );

        ServerPlayer target = source.getServer().getPlayerList().getPlayer(targetId);
        if (target != null) {
            target.sendSystemMessage(CointCoreMessages.forPlayer(target, CointCoreMessages.MUTE_LIFTED));
        }

        return true;
    }

    public static void notifyIfMuted(ServerPlayer player) {
        MuteManager.getMute(player.server, player.getUUID()).ifPresent(info -> player.sendSystemMessage(
                CointCoreMessages.forPlayer(
                        player,
                        CointCoreMessages.MUTE_BLOCKED,
                        TimeUtil.formatDuration(info.remainingMs())
                )
        ));
    }

    public static long parseDurationMs(String time) throws CommandSyntaxException {
        String token = time.toLowerCase(Locale.ROOT);
        if (token.equals("perm") || token.equals("forever") || token.equals("permanent")) {
            return 0L;
        }

        if (token.length() < 2) {
            throw INVALID_DURATION.create();
        }

        char unit = token.charAt(token.length() - 1);
        long amount;
        try {
            amount = Long.parseLong(token.substring(0, token.length() - 1));
        } catch (NumberFormatException exception) {
            throw INVALID_DURATION.create();
        }

        if (amount <= 0L) {
            throw INVALID_DURATION.create();
        }

        return switch (unit) {
            case 's' -> amount * 1000L;
            case 'm' -> amount * 60L * 1000L;
            case 'h' -> amount * 60L * 60L * 1000L;
            case 'd' -> amount * 24L * 60L * 60L * 1000L;
            case 'w' -> amount * 7L * 24L * 60L * 60L * 1000L;
            default -> throw INVALID_DURATION.create();
        };
    }
}
