package com.mawlee.cointcore.vote;

import com.mawlee.cointcore.config.VoteConfig;
import com.mawlee.cointcore.environment.TimeWeatherCooldown;
import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class VoteService {
    private static final Map<VoteType, ActiveVote> ACTIVE = new EnumMap<>(VoteType.class);

    private VoteService() {
    }

    public static int castVote(VoteType type, ServerPlayer player) {
        MinecraftServer server = player.server;
        long now = System.currentTimeMillis();
        int environmentCooldownLeft = TimeWeatherCooldown.remainingSeconds();
        if (environmentCooldownLeft > 0) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(
                    player,
                    CointCoreMessages.ENVIRONMENT_COOLDOWN,
                    environmentCooldownLeft
            ));
            return 0;
        }

        ActiveVote active = ACTIVE.get(type);
        if (active != null && active.isExpired(now)) {
            ACTIVE.remove(type);
            active = null;
        }

        UUID playerId = player.getUUID();
        if (active != null) {
            if (active.voters.contains(playerId)) {
                player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.VOTE_ALREADY_VOTED));
                return 0;
            }
            active.voters.add(playerId);
        } else {
            VoteConfig.VoteSettings settings = VoteConfig.getSettings(type);
            active = new ActiveVote(type, settings, now + settings.durationSeconds() * 1000L);
            active.voters.add(playerId);
            ACTIVE.put(type, active);
            broadcast(
                    server,
                    CointCoreMessages.VOTE_STARTED,
                    type,
                    active.requiredThreshold(server),
                    settings.requiredPercentage(),
                    settings.durationSeconds()
            );
        }

        int threshold = active.requiredThreshold(server);
        int current = active.voters.size();
        broadcast(server, CointCoreMessages.VOTE_PROGRESS, type, current, threshold);

        if (current >= threshold) {
            completeVote(server, active);
        }
        return 1;
    }

    public static void applySleepPercentage(MinecraftServer server) {
        int percentage = VoteConfig.getSleepPercentage();
        for (ServerLevel level : server.getAllLevels()) {
            level.getGameRules().getRule(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE).set(percentage, server);
        }
    }

    public static void clearRuntimeState() {
        ACTIVE.clear();
        TimeWeatherCooldown.clearRuntimeState();
    }

    private static void completeVote(MinecraftServer server, ActiveVote active) {
        ACTIVE.remove(active.type);
        broadcast(server, CointCoreMessages.VOTE_PASSED, active.type);

        boolean applied = TimeWeatherCooldown.runForced(() -> {
            if (active.type == VoteType.DAY) {
                applyDayVote(server);
            } else if (active.type == VoteType.CLEAR_WEATHER) {
                applyClearWeatherVote(server);
            }
        });
        if (!applied) {
            // Race: another forced change started between the vote check and apply.
            broadcast(server, CointCoreMessages.ENVIRONMENT_COOLDOWN_BLOCKED, active.type);
        }
    }

    private static void applyDayVote(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            overworld.setDayTime(1000L);
            overworld.setWeatherParameters(6000, 0, false, false);
        }
    }

    private static void applyClearWeatherVote(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            level.setWeatherParameters(6000, 0, false, false);
        }
    }

    private static void broadcast(MinecraftServer server, String messageKey, VoteType type, Object... args) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Object[] formattedArgs = prependTypeLabel(player, type, args);
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, messageKey, formattedArgs));
        }
    }

    private static void broadcast(MinecraftServer server, String messageKey, Object... args) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, messageKey, args));
        }
    }

    private static Object[] prependTypeLabel(ServerPlayer player, VoteType type, Object... args) {
        String typeLabel = CointCoreMessages.translateKey(player.getLanguage(), typeLabelKey(type));
        Object[] formatted = new Object[args.length + 1];
        formatted[0] = typeLabel;
        System.arraycopy(args, 0, formatted, 1, args.length);
        return formatted;
    }

    private static String typeLabelKey(VoteType type) {
        if (type == VoteType.DAY) {
            return CointCoreMessages.VOTE_TYPE_DAY;
        }
        if (type == VoteType.CLEAR_WEATHER) {
            return CointCoreMessages.VOTE_TYPE_CLEAR_WEATHER;
        }
        return CointCoreMessages.VOTE_TYPE_DAY;
    }

    private static final class ActiveVote {
        private final VoteType type;
        private final VoteConfig.VoteSettings settings;
        private final long expiresAtMs;
        private final Set<UUID> voters = new HashSet<>();

        private ActiveVote(VoteType type, VoteConfig.VoteSettings settings, long expiresAtMs) {
            this.type = type;
            this.settings = settings;
            this.expiresAtMs = expiresAtMs;
        }

        private boolean isExpired(long now) {
            return now >= expiresAtMs;
        }

        private int requiredThreshold(MinecraftServer server) {
            int online = countVotingPlayers(server);
            return Math.max(1, (int) Math.ceil(online * settings.requiredPercentage() / 100.0D));
        }
    }

    private static int countVotingPlayers(MinecraftServer server) {
        return (int) server.getPlayerList().getPlayers().stream()
                .filter(player -> !player.isSpectator())
                .count();
    }
}
