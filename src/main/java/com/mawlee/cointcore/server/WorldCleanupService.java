package com.mawlee.cointcore.server;

import com.mawlee.cointcore.config.MobCleanupConfig;
import com.mawlee.cointcore.config.WorldCleanupConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.lang.LegacyTextParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Consumer;

public final class WorldCleanupService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static long lastPeriodicCheckMs;
    private static int lastItemCount;
    private static boolean clearSequenceActive;
    private static final PriorityQueue<ScheduledTask> scheduledTasks = new PriorityQueue<>();

    private WorldCleanupService() {
    }

    public static int getLastItemCount() {
        return lastItemCount;
    }

    public static void resetRuntimeState() {
        lastPeriodicCheckMs = 0L;
        lastItemCount = 0;
        clearSequenceActive = false;
        scheduledTasks.clear();
    }

    public static void tick(MinecraftServer server) {
        long tick = server.getTickCount();
        while (!scheduledTasks.isEmpty() && scheduledTasks.peek().runAtTick() <= tick) {
            scheduledTasks.poll().action().accept(server);
        }

        if (tick % 20L != 0L) {
            return;
        }

        WorldCleanupConfig.ItemClearSettings settings = WorldCleanupConfig.getItemClear();
        if (!settings.enabled() || clearSequenceActive) {
            return;
        }

        long intervalMs = settings.checkIntervalMinutes() * 60_000L;
        if (intervalMs <= 0L) {
            intervalMs = 10_000L;
        }

        long now = System.currentTimeMillis();
        if (now - lastPeriodicCheckMs < intervalMs) {
            return;
        }

        lastPeriodicCheckMs = now;
        server.execute(() -> runItemCountCheck(server));
    }

    public static void triggerManualStatus(MinecraftServer server) {
        server.execute(() -> {
            List<ServerLevel> levels = resolveLevels(server);
            if (levels.isEmpty()) {
                LOGGER.warn("World cleanup skipped: no configured dimensions are loaded");
                return;
            }
            lastItemCount = countItemEntities(levels);
            broadcastManualCheckStatus(server);
        });
    }

    public static void triggerManualCleanup(MinecraftServer server) {
        server.execute(() -> {
            if (clearSequenceActive) {
                LOGGER.warn("World cleanup already in progress");
                return;
            }

            List<ServerLevel> levels = resolveLevels(server);
            if (levels.isEmpty()) {
                LOGGER.warn("World cleanup skipped: no configured dimensions are loaded");
                return;
            }

            lastItemCount = countItemEntities(levels);
            executeClear(server, levels, false, true);
        });
    }

    public static void broadcastManualCheckStatus(MinecraftServer server) {
        WorldCleanupConfig.ItemClearSettings settings = WorldCleanupConfig.getItemClear();
        server.getPlayerList().broadcastSystemMessage(
                CointCoreMessages.forConsole(
                        CointCoreMessages.WORLD_CLEANUP_MANUAL_STATUS,
                        lastItemCount,
                        settings.itemThreshold()
                ),
                false
        );
    }

    private static void runItemCountCheck(MinecraftServer server) {
        List<ServerLevel> levels = resolveLevels(server);
        if (levels.isEmpty()) {
            LOGGER.warn("World cleanup skipped: no configured dimensions are loaded");
            return;
        }

        lastItemCount = countItemEntities(levels);
        WorldCleanupConfig.ItemClearSettings settings = WorldCleanupConfig.getItemClear();
        if (settings.requireItemThreshold() && lastItemCount < settings.itemThreshold()) {
            return;
        }

        scheduleClearSequence(server, levels);
    }

    private static List<ServerLevel> resolveLevels(MinecraftServer server) {
        List<ServerLevel> levels = new ArrayList<>();
        for (ResourceKey<Level> dimension : WorldCleanupConfig.getItemClear().checkDimensions()) {
            ServerLevel level = server.getLevel(dimension);
            if (level != null) {
                levels.add(level);
            } else {
                LOGGER.warn("World cleanup dimension {} is not loaded", dimension.location());
            }
        }
        return levels;
    }

    private static void scheduleClearSequence(MinecraftServer server, List<ServerLevel> levels) {
        if (clearSequenceActive) {
            return;
        }

        WorldCleanupConfig.ItemClearSettings settings = WorldCleanupConfig.getItemClear();
        List<Integer> warnings = new ArrayList<>(settings.warningsSecondsBefore());
        warnings.sort(Comparator.naturalOrder());

        if (warnings.isEmpty()) {
            executeClear(server, levels, true, false);
            return;
        }

        clearSequenceActive = true;
        int maxWarningSeconds = warnings.getLast();
        long baseTick = server.getTickCount();

        if (settings.showTitleOnFirstWarning() && !server.getPlayerList().getPlayers().isEmpty()) {
            showTitle(server, settings.titleText());
        }

        for (int warningSeconds : warnings) {
            if (warningSeconds == maxWarningSeconds) {
                schedule(server, baseTick + warningSeconds * 20L, currentServer -> {
                    executeClear(currentServer, levels, true, false);
                    clearSequenceActive = false;
                });
            }

            long delayTicks = (maxWarningSeconds - warningSeconds) * 20L;
            String warningText = settings.warningText().replaceFirst("%", String.valueOf(warningSeconds));
            schedule(server, baseTick + delayTicks, currentServer -> broadcastWarning(currentServer, warningText));
        }
    }

    private static void executeClear(MinecraftServer server, List<ServerLevel> levels, boolean automatic, boolean forced) {
        int itemCount = countItemEntities(levels);
        WorldCleanupConfig.ItemClearSettings settings = WorldCleanupConfig.getItemClear();
        boolean shouldClear = forced
                || automatic
                || !settings.requireItemThreshold()
                || itemCount >= settings.itemThreshold();

        if (!shouldClear) {
            server.getPlayerList().broadcastSystemMessage(
                    CointCoreMessages.forConsole(CointCoreMessages.WORLD_CLEANUP_PREVENTED, itemCount),
                    false
            );
            return;
        }

        int mobCount = 0;
        for (ServerLevel level : levels) {
            for (ItemEntity item : collectItemEntities(level)) {
                item.remove(Entity.RemovalReason.DISCARDED);
            }
            mobCount += removeConfiguredMobs(level);
        }

        server.getPlayerList().broadcastSystemMessage(
                CointCoreMessages.forConsole(
                        CointCoreMessages.WORLD_CLEANUP_CLEARED,
                        itemCount,
                        mobCount
                ),
                false
        );
    }

    private static int countItemEntities(List<ServerLevel> levels) {
        int count = 0;
        for (ServerLevel level : levels) {
            count += countItemEntities(level);
        }
        return count;
    }

    private static int countItemEntities(ServerLevel level) {
        int count = 0;
        for (Entity entity : level.getEntities().getAll()) {
            if (entity.getType() == EntityType.ITEM) {
                count++;
            }
        }
        return count;
    }

    private static List<ItemEntity> collectItemEntities(ServerLevel level) {
        List<ItemEntity> items = new ArrayList<>();
        for (Entity entity : level.getEntities().getAll()) {
            if (entity instanceof ItemEntity itemEntity) {
                items.add(itemEntity);
            }
        }
        return items;
    }

    private static int removeConfiguredMobs(ServerLevel level) {
        List<Entity> toRemove = new ArrayList<>();
        for (Entity entity : level.getEntities().getAll()) {
            if (shouldRemoveMob(entity)) {
                toRemove.add(entity);
            }
        }
        for (Entity entity : toRemove) {
            entity.remove(Entity.RemovalReason.DISCARDED);
        }
        return toRemove.size();
    }

    private static boolean shouldRemoveMob(Entity entity) {
        if (entity == null || entity instanceof Player || isAdAstraVehicle(entity)) {
            return false;
        }
        if (MobCleanupConfig.matchesExcludedType(entity.getType())) {
            return false;
        }
        if (MobCleanupConfig.matchesConfiguredType(entity.getType())) {
            return true;
        }
        return MobCleanupConfig.useHostileFallback()
                && entity.getType().getCategory() == MobCategory.MONSTER;
    }

    /**
     * Rockets, rovers, landers, and their multipart pieces are entities.
     * Item clearing only removes {@link ItemEntity}; this also keeps a configured
     * mob list from discarding a vehicle.
     */
    private static boolean isAdAstraVehicle(Entity entity) {
        Class<?> type = entity.getClass();
        while (type != null && type != Entity.class) {
            String name = type.getName();
            if (name.startsWith("earth.terrarium.adastra.common.entities.vehicles.")) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static void broadcastWarning(MinecraftServer server, String warningText) {
        MutableComponent message = Component.literal("[===");
        message.append(LegacyTextParser.parse(warningText).copy().withStyle(ChatFormatting.RED));
        message.append(Component.literal("===]"));
        server.getPlayerList().broadcastSystemMessage(message, false);
    }

    private static void showTitle(MinecraftServer server, String titleText) {
        String escaped = titleText
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        String command = "title @a title {\"text\":\"" + escaped + "\",\"color\":\"dark_red\"}";
        try {
            server.getCommands().getDispatcher().execute(command, server.createCommandSourceStack());
        } catch (CommandSyntaxException exception) {
            LOGGER.warn("Failed to run world cleanup title command", exception);
        }
    }

    private static void schedule(MinecraftServer server, long runAtTick, Consumer<MinecraftServer> action) {
        scheduledTasks.add(new ScheduledTask(runAtTick, action));
    }

    private record ScheduledTask(long runAtTick, Consumer<MinecraftServer> action) implements Comparable<ScheduledTask> {
        @Override
        public int compareTo(ScheduledTask other) {
            return Long.compare(runAtTick, other.runAtTick);
        }
    }
}
