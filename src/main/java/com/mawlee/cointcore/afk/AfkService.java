package com.mawlee.cointcore.afk;

import com.mawlee.cointcore.config.AfkConfig;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * AFK timers arm on join / strong activity.
 * Weak activity never resets timers; while marked AFK it is treated as suspicious.
 */
public final class AfkService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float INPUT_EPSILON = 1.0E-3F;

    public static final String ACTIVITY_LOOK = "поворот камеры";
    public static final String ACTIVITY_MOVEMENT = "движение/прыжок";
    public static final String ACTIVITY_GUI = "активность в GUI/инвентаре";
    public static final String ACTIVITY_CHAT = "чат";
    public static final String ACTIVITY_COMMAND = "команда";
    public static final String ACTIVITY_PLAYER_ACTION = "действие игрока (копание/использование)";
    public static final String ACTIVITY_USE_ITEM = "использование предмета";
    public static final String ACTIVITY_USE_ITEM_ON = "использование предмета по блоку";
    public static final String ACTIVITY_INTERACT = "взаимодействие с сущностью";
    public static final String ACTIVITY_SWING = "взмах рукой";
    public static final String ACTIVITY_HOTBAR = "смена слота хотбара";
    public static final String ACTIVITY_CONTAINER_CLICK = "клик по инвентарю/контейнеру";
    public static final String ACTIVITY_CONTAINER_CLOSE = "закрытие контейнера";
    public static final String ACTIVITY_BYPASS = "bypass-право (AFK сброшен)";

    private AfkService() {
    }

    public static boolean canBypass(ServerPlayer player) {
        return PermissionService.has(player, CointPermissionNodes.AFK_BYPASS);
    }

    public static boolean canReceiveSuspectAlerts(ServerPlayer player) {
        return PermissionService.has(player, CointPermissionNodes.AFK_ALERTS)
                || PermissionService.has(player, CointPermissionNodes.ADMIN_CHAT);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        AfkTracker.clear(player.getUUID());
        if (!AfkConfig.isEnabled() || canBypass(player)) {
            return;
        }
        armTimers(AfkTracker.getOrCreate(player), System.currentTimeMillis());
    }

    public static void onPlayerLeave(ServerPlayer player) {
        AfkTracker.clear(player.getUUID());
    }

    public static void clearRuntimeState() {
        AfkTracker.clearAll();
    }

    public static void rearmAllOnline(MinecraftServer server) {
        if (server == null) {
            AfkTracker.clearAll();
            return;
        }
        if (!AfkConfig.isEnabled()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                AfkTracker.PlayerState state = AfkTracker.get(player.getUUID());
                if (state != null && state.marked) {
                    state.disarm();
                    refreshTab(player);
                }
            }
            AfkTracker.clearAll();
            return;
        }

        long now = System.currentTimeMillis();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            AfkTracker.PlayerState state = AfkTracker.getOrCreate(player);
            if (canBypass(player)) {
                boolean wasMarked = state.marked;
                state.disarm();
                if (wasMarked) {
                    refreshTab(player);
                    logUnmarked(player, ACTIVITY_BYPASS);
                }
                continue;
            }

            boolean wasMarked = state.marked;
            long base = state.lastActivityMs > 0L ? state.lastActivityMs : now;
            state.arm(base, AfkConfig.getMarkAfterSeconds(), AfkConfig.getKickAfterSeconds());
            if (wasMarked && now < state.markDueMs) {
                refreshTab(player);
                logUnmarked(player, "reload конфига");
            }
        }
        AfkTracker.recomputeNextDue();
    }

    public static void touch(ServerPlayer player) {
        touch(player, "активность", AfkActivityStrength.STRONG);
    }

    public static void touch(ServerPlayer player, String activity) {
        touch(player, activity, AfkActivityStrength.STRONG);
    }

    public static void touch(ServerPlayer player, String activity, AfkActivityStrength strength) {
        if (player == null || !AfkConfig.isEnabled() || canBypass(player) || strength == null) {
            return;
        }

        if (strength == AfkActivityStrength.WEAK) {
            onWeakActivity(player, activity);
            return;
        }

        AfkTracker.PlayerState state = AfkTracker.getOrCreate(player);
        boolean wasMarked = state.marked;
        armTimers(state, System.currentTimeMillis());
        if (wasMarked) {
            refreshTab(player);
            logUnmarked(player, activity);
        }
    }

    public static void onGuiHeartbeat(ServerPlayer player) {
        if (player == null || !AfkConfig.isEnabled() || !AfkConfig.isGuiHeartbeatEnabled() || canBypass(player)) {
            return;
        }
        if (player.containerMenu == player.inventoryMenu) {
            return;
        }
        touch(player, ACTIVITY_GUI, AfkActivityStrength.WEAK);
    }

    public static void onMoveLook(ServerPlayer player, ServerboundMovePlayerPacket packet) {
        if (player == null || !AfkConfig.isEnabled() || canBypass(player) || !packet.hasRotation()) {
            return;
        }

        float yaw = packet.getYRot(player.getYRot());
        float pitch = packet.getXRot(player.getXRot());
        AfkTracker.PlayerState state = AfkTracker.getOrCreate(player);
        if (state.updateLook(yaw, pitch, AfkConfig.getLookThresholdDegrees())) {
            touch(player, ACTIVITY_LOOK, AfkActivityStrength.STRONG);
        }
    }

    public static void onPlayerInput(ServerPlayer player, ServerboundPlayerInputPacket packet) {
        if (player == null || !AfkConfig.isEnabled() || canBypass(player)) {
            return;
        }

        if (Math.abs(packet.getXxa()) > INPUT_EPSILON
                || Math.abs(packet.getZza()) > INPUT_EPSILON
                || packet.isJumping()) {
            touch(player, ACTIVITY_MOVEMENT, AfkActivityStrength.STRONG);
        }
    }

    public static void tick(MinecraftServer server) {
        if (!AfkConfig.isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < AfkTracker.nextDueMs()) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            AfkTracker.PlayerState state = AfkTracker.get(player.getUUID());
            if (state == null) {
                if (canBypass(player)) {
                    continue;
                }
                state = AfkTracker.getOrCreate(player);
                armTimers(state, now);
                continue;
            }

            if (canBypass(player)) {
                if (state.armed || state.marked) {
                    boolean wasMarked = state.marked;
                    state.disarm();
                    if (wasMarked) {
                        refreshTab(player);
                        logUnmarked(player, ACTIVITY_BYPASS);
                    }
                    AfkTracker.recomputeNextDue();
                }
                continue;
            }

            if (!state.armed) {
                armTimers(state, now);
                continue;
            }

            // Mark before kick so equal thresholds / lag skips still log AFK to console.
            if (!state.marked && now >= state.markDueMs) {
                markAfk(player, state, now);
            }

            if (now >= state.kickDueMs) {
                if (!state.marked) {
                    markAfk(player, state, now);
                }
                long idleSeconds = Math.max(0L, (now - state.lastActivityMs) / 1000L);
                kick(player, idleSeconds, AfkConfig.getKickAfterSeconds());
            }
        }

        AfkTracker.recomputeNextDue();
    }

    private static void markAfk(ServerPlayer player, AfkTracker.PlayerState state, long now) {
        if (state.marked) {
            return;
        }
        state.marked = true;
        state.weakActionsWhileAfk = 0;
        state.lastSuspectNotifyMs = 0L;
        refreshTab(player);
        long untilKick = Math.max(0L, (state.kickDueMs - now) / 1000L);
        String name = player.getGameProfile().getName();
        LOGGER.info(
                "AFK: игрок {} помечен как AFK (порог mark={}s, кик через {}s)",
                name,
                AfkConfig.getMarkAfterSeconds(),
                untilKick
        );
        // Explicit console line — same visibility as vanilla server messages.
        System.out.println("[CointCore] AFK: игрок " + name + " помечен как AFK (кик через " + untilKick + "s)");
    }

    private static void onWeakActivity(ServerPlayer player, String activity) {
        AfkTracker.PlayerState state = AfkTracker.get(player.getUUID());
        if (state == null || !state.marked) {
            return;
        }

        state.weakActionsWhileAfk++;
        int count = state.weakActionsWhileAfk;
        String name = player.getGameProfile().getName();
        LOGGER.info("AFK-SUSPECT: игрок {} слабое действие «{}» (#{}) во время AFK", name, activity, count);

        int threshold = AfkConfig.getSuspectWeakActionsThreshold();
        if (count < threshold) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownMs = AfkConfig.getSuspectNotifyCooldownSeconds() * 1000L;
        if (state.lastSuspectNotifyMs > 0L && now - state.lastSuspectNotifyMs < cooldownMs) {
            return;
        }

        state.lastSuspectNotifyMs = now;
        LOGGER.warn(
                "AFK-SUSPECT: игрок {} — ручная проверка ({} слабых действий во время AFK, последнее: {})",
                name,
                count,
                activity
        );
        notifyAdmins(player, count, activity);
    }

    private static void notifyAdmins(ServerPlayer target, int weakCount, String activity) {
        MinecraftServer server = target.server;
        String targetName = target.getGameProfile().getName();

        for (ServerPlayer admin : server.getPlayerList().getPlayers()) {
            if (!canReceiveSuspectAlerts(admin) || admin.getUUID().equals(target.getUUID())) {
                continue;
            }

            MutableComponent message = CointCoreMessages.forPlayer(
                    admin,
                    CointCoreMessages.AFK_SUSPECT,
                    targetName,
                    weakCount,
                    activity
            ).copy();

            MutableComponent tpLink = CointCoreMessages.forPlayer(admin, CointCoreMessages.AFK_SUSPECT_TP).copy();
            Component hover = CointCoreMessages.forPlayer(admin, CointCoreMessages.AFK_SUSPECT_TP_HOVER, targetName);
            tpLink.withStyle(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpl " + targetName))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));

            admin.sendSystemMessage(message.append(tpLink));
        }
    }

    private static void armTimers(AfkTracker.PlayerState state, long nowMs) {
        state.arm(nowMs, AfkConfig.getMarkAfterSeconds(), AfkConfig.getKickAfterSeconds());
        AfkTracker.recomputeNextDue();
    }

    private static void kick(ServerPlayer player, long idleSeconds, int kickAfter) {
        Component kickMessage;
        try {
            kickMessage = CointCoreMessages.forPlayer(player, CointCoreMessages.AFK_KICK);
        } catch (Throwable ignored) {
            kickMessage = Component.literal("Kicked for being AFK too long.");
        }
        LOGGER.info(
                "AFK: кик игрока {} (бездействие {}s, порог kick={}s)",
                player.getGameProfile().getName(),
                idleSeconds,
                kickAfter
        );
        AfkTracker.clear(player.getUUID());
        player.connection.disconnect(kickMessage);
    }

    private static void logUnmarked(ServerPlayer player, String activity) {
        LOGGER.info(
                "AFK: игрок {} снят с AFK (действие: {})",
                player.getGameProfile().getName(),
                activity != null && !activity.isBlank() ? activity : "активность"
        );
    }

    private static void refreshTab(ServerPlayer player) {
        try {
            player.refreshTabListName();
        } catch (RuntimeException exception) {
            LOGGER.debug("Failed to refresh tab list name for {}", player.getGameProfile().getName(), exception);
        }
    }
}
