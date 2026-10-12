package com.mawlee.cointcore.questrank;

import com.mawlee.cointcore.luckperms.PublicApiMethodResolver;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Soft-dep access to FTB Quests team completion and FTB Teams members.
 * Quests are reached through {@link PublicApiMethodResolver} so the API jar is optional.
 *
 * <p>Ranks are granted on login and by a periodic sweep. Do not hook FTB Quests
 * {@code ObjectCompletedEvent}: Architectury {@code Event&lt;EventActor&lt;...&gt;&gt;}
 * casts listeners to {@code EventActor}, and a reflective proxy of the
 * wrong interface crashes the server on every quest complete.
 */
public final class FtbQuestProgress {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String QUESTS_API = "dev.ftb.mods.ftbquests.api.FTBQuestsAPI";

    private FtbQuestProgress() {
    }

    public static boolean isQuestsLoaded() {
        return ModList.get().isLoaded("ftbquests");
    }

    public static boolean isTeamsLoaded() {
        return ModList.get().isLoaded("ftbteams");
    }

    public static boolean isAvailable() {
        return isQuestsLoaded() && isTeamsLoaded();
    }

    public static Optional<Team> teamOf(ServerPlayer player) {
        if (player == null || !isTeamsLoaded()) {
            return Optional.empty();
        }
        try {
            if (!FTBTeamsAPI.api().isManagerLoaded()) {
                return Optional.empty();
            }
            return FTBTeamsAPI.api().getManager().getTeamForPlayer(player);
        } catch (RuntimeException exception) {
            LOGGER.debug("FTB Teams lookup failed for {}", player.getUUID(), exception);
            return Optional.empty();
        }
    }

    public static Optional<UUID> teamId(ServerPlayer player) {
        return teamOf(player).map(Team::getId);
    }

    public static List<ServerPlayer> onlineTeamMembers(ServerPlayer player) {
        Optional<Team> team = teamOf(player);
        if (team.isEmpty()) {
            return player == null ? List.of() : List.of(player);
        }
        return onlineMembers(player.server, team.get());
    }

    public static List<ServerPlayer> onlineMembers(MinecraftServer server, Team team) {
        if (server == null || team == null) {
            return List.of();
        }
        List<ServerPlayer> online = new ArrayList<>();
        for (UUID memberId : team.getMembers()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                online.add(member);
            }
        }
        return List.copyOf(online);
    }

    public static Set<Long> completedMapped(ServerPlayer player, Set<Long> mappedIds) {
        if (player == null || mappedIds == null || mappedIds.isEmpty() || !isQuestsLoaded()) {
            return Set.of();
        }
        Object file = questFile();
        if (file == null) {
            return Set.of();
        }
        Object teamData = teamData(file, player);
        if (teamData == null) {
            return Set.of();
        }
        Set<Long> completed = new LinkedHashSet<>();
        for (long questId : mappedIds) {
            if (isCompleted(file, teamData, questId)) {
                completed.add(questId);
            }
        }
        return Set.copyOf(completed);
    }

    private static Object questFile() {
        try {
            Class<?> apiClass = Class.forName(QUESTS_API);
            Object api = apiClass.getMethod("api").invoke(null);
            if (api == null) {
                return null;
            }
            return PublicApiMethodResolver.invoke(api, "getQuestFile", new Class<?>[]{boolean.class}, false);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.debug("FTB Quests file lookup failed", exception);
            return null;
        }
    }

    private static Object teamData(Object file, ServerPlayer player) {
        Object byPlayer = invokeOrNull(file, "getTeamData", player);
        if (byPlayer instanceof Optional<?> optional) {
            if (optional.isPresent()) {
                return optional.get();
            }
        } else if (byPlayer != null) {
            return byPlayer;
        }
        Optional<UUID> teamId = teamId(player);
        if (teamId.isPresent()) {
            Object byTeam = firstNonNull(
                    invokeOrNull(file, "getOrCreateTeamData", teamId.get()),
                    invokeOrNull(file, "getNullableTeamData", teamId.get()),
                    invokeOrNull(file, "getTeamData", teamId.get())
            );
            if (byTeam instanceof Optional<?> optional) {
                return optional.orElse(null);
            }
            if (byTeam != null) {
                return byTeam;
            }
        }
        return firstNonNull(
                invokeOrNull(file, "getOrCreateTeamData", player),
                invokeOrNull(file, "getTeamData", player.getUUID())
        );
    }

    private static boolean isCompleted(Object file, Object teamData, long questId) {
        Object direct = invokeOrNull(teamData, "isCompleted", questId);
        if (direct instanceof Boolean value) {
            return value;
        }
        Object quest = firstNonNull(
                invokeOrNull(file, "get", questId),
                invokeOrNull(file, "getQuest", questId)
        );
        if (quest == null) {
            return false;
        }
        Object byObject = invokeOrNull(teamData, "isCompleted", quest);
        return Boolean.TRUE.equals(byObject);
    }

    private static Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Object invokeOrNull(Object target, String methodName, Object... args) {
        if (target == null) {
            return null;
        }
        try {
            if (args.length == 1 && args[0] != null) {
                return PublicApiMethodResolver.invoke(
                        target,
                        methodName,
                        new Class<?>[]{args[0].getClass()},
                        args
                );
            }
            return PublicApiMethodResolver.invoke(target, methodName, args);
        } catch (ReflectiveOperationException ignored) {
            if (args.length == 1 && args[0] instanceof Long questId) {
                try {
                    return PublicApiMethodResolver.invoke(
                            target,
                            methodName,
                            new Class<?>[]{long.class},
                            questId
                    );
                } catch (ReflectiveOperationException ignoredAgain) {
                    return null;
                }
            }
            if (args.length == 1 && args[0] != null) {
                try {
                    return PublicApiMethodResolver.invoke(target, methodName, args);
                } catch (ReflectiveOperationException ignoredAgain) {
                    return null;
                }
            }
            return null;
        }
    }
}
