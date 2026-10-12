package com.mawlee.cointcore.questrank;

import com.mawlee.cointcore.luckperms.PublicApiMethodResolver;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Soft-dep access to FTB Quests team completion and FTB Teams members.
 * Quests are reached through {@link PublicApiMethodResolver} so the API jar is optional.
 */
public final class FtbQuestProgress {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String QUESTS_API = "dev.ftb.mods.ftbquests.api.FTBQuestsAPI";
    private static final String COMPLETED_EVENT = "dev.ftb.mods.ftbquests.events.ObjectCompletedEvent";
    private static final String ARCHITECTURY_EVENT = "dev.architectury.event.Event";
    private static boolean completionHookLogged;

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

    public static void registerCompletionListener(Consumer<List<ServerPlayer>> listener) {
        if (!isQuestsLoaded()) {
            logCompletionHook("FTB Quests is not loaded; quest-rank grants use login and sweep only");
            return;
        }
        try {
            if (subscribeCompletedEvent(listener)) {
                LOGGER.info("Quest-rank hook registered on FTB Quests ObjectCompletedEvent");
                return;
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.info("Quest-rank completion event hook failed: {}", exception.toString());
        }
        logCompletionHook("FTB Quests completion event unavailable; login and periodic sweep still grant ranks");
    }

    private static boolean subscribeCompletedEvent(Consumer<List<ServerPlayer>> listener)
            throws ReflectiveOperationException {
        Class<?> eventClass = Class.forName(COMPLETED_EVENT);
        Object bus = questEventBus(eventClass);
        if (bus == null) {
            return false;
        }
        Class<?> listenerType = listenerType(eventClass, bus);
        Object proxy = Proxy.newProxyInstance(
                eventClass.getClassLoader(),
                new Class<?>[]{listenerType},
                completionHandler(listener)
        );
        Class<?> architectury = Class.forName(ARCHITECTURY_EVENT);
        architectury.getMethod("register", Object.class).invoke(bus, proxy);
        return true;
    }

    private static Object questEventBus(Class<?> eventClass) throws ReflectiveOperationException {
        for (String name : List.of("QUEST", "GENERIC")) {
            try {
                Field field = eventClass.getField(name);
                return field.get(null);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static Class<?> listenerType(Class<?> eventClass, Object bus) {
        for (Class<?> inner : eventClass.getDeclaredClasses()) {
            if (inner.isInterface()) {
                return inner;
            }
        }
        Class<?>[] interfaces = bus.getClass().getInterfaces();
        if (interfaces.length > 0) {
            return Consumer.class;
        }
        return Consumer.class;
    }

    private static InvocationHandler completionHandler(Consumer<List<ServerPlayer>> listener) {
        return (proxy, method, args) -> {
            if (args != null && args.length == 1 && method.getDeclaringClass() != Object.class) {
                List<ServerPlayer> members = onlineMembersFromEvent(args[0]);
                if (!members.isEmpty()) {
                    listener.accept(members);
                }
            }
            return defaultReturn(method);
        };
    }

    private static Object defaultReturn(Method method) throws ReflectiveOperationException {
        Class<?> type = method.getReturnType();
        if (type == void.class) {
            return null;
        }
        if (type.getName().equals("dev.architectury.event.EventResult")) {
            return type.getMethod("pass").invoke(null);
        }
        if (type == boolean.class) {
            return Boolean.TRUE;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<ServerPlayer> onlineMembersFromEvent(Object event) {
        if (event == null) {
            return List.of();
        }
        List<ServerPlayer> members = new ArrayList<>();
        Object[] sources = new Object[]{event, invokeOrNull(event, "getData")};
        for (Object source : sources) {
            if (source == null) {
                continue;
            }
            Object online = invokeOrNull(source, "getOnlineMembers");
            if (online instanceof Collection<?> collection) {
                for (Object value : collection) {
                    if (value instanceof ServerPlayer player && !members.contains(player)) {
                        members.add(player);
                    }
                }
            }
        }
        return List.copyOf(members);
    }

    private static Object questFile() {
        try {
            Class<?> apiClass = Class.forName(QUESTS_API);
            Object api = PublicApiMethodResolver.invoke(apiClass, null, "api");
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

    private static void logCompletionHook(String message) {
        if (!completionHookLogged) {
            completionHookLogged = true;
            LOGGER.info(message);
        }
    }
}
