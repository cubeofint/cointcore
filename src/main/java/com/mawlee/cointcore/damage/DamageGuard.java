package com.mawlee.cointcore.damage;

import com.mawlee.cointcore.config.DamageGuardConfig;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.watchdog.stall.StallContext;
import com.mawlee.cointcore.watchdog.stall.StallWatchdogHotFlags;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Locale;

/**
 * Server-thread damage recursion / burst guard. Hot path is primitive counters only.
 */
public final class DamageGuard {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DamageGuardCounters COUNTERS = new DamageGuardCounters();
    private static final Long2LongOpenHashMap LAST_LOG_MS = new Long2LongOpenHashMap();
    private static long lastLeakLogMs;

    static {
        LAST_LOG_MS.defaultReturnValue(0L);
    }

    private DamageGuard() {
    }

    public static DamageGuardCounters counters() {
        return COUNTERS;
    }

    public static void onServerTick() {
        COUNTERS.resetTick();
        // No hurt() is in progress between ticks. A non-zero depth here means an exit was skipped (another
        // mod cancelled hurt() at HEAD after our enter, or hurt() threw); left alone it only grows until
        // every hurt() on the server trips maxDepth and all entities become immortal.
        int leaked = COUNTERS.resetDepth();
        if (leaked > 0) {
            StallContext.exitDamage();
            logLeakedDepth(leaked);
        }
    }

    /**
     * @return {@code true} if the hurt call should be cancelled. A cancelled call never reaches the
     * {@code hurt()} RETURN injection, so it must not leave the depth counter raised.
     */
    public static boolean onHurtEnter(LivingEntity target, DamageSource source) {
        if (target == null || target.level().isClientSide() || !DamageGuardConfig.isEnabled()) {
            return false;
        }
        if (StallWatchdogHotFlags.ABORT_DAMAGE && COUNTERS.currentDepth() > 0) {
            return true;
        }
        int depth = COUNTERS.enter();
        StallContext.enterDamage(target, source);
        int hits = COUNTERS.recordHit(target.getId());
        int maxDepth = DamageGuardConfig.getMaxDepth();
        int maxHits = DamageGuardConfig.getMaxHitsPerTargetPerTick();
        if (depth > maxDepth || hits > maxHits) {
            COUNTERS.markBlocked(target.getId());
            trip(target, source, depth, hits);
            onHurtExit();
            return true;
        }
        return false;
    }

    public static void onHurtExit() {
        COUNTERS.exit();
        if (COUNTERS.currentDepth() == 0) {
            StallContext.exitDamage();
        }
    }

    public static boolean shouldCancelIncoming(LivingEntity target) {
        if (target == null || target.level().isClientSide() || !DamageGuardConfig.isEnabled()) {
            return false;
        }
        if (StallWatchdogHotFlags.ABORT_DAMAGE && COUNTERS.currentDepth() > 0) {
            return true;
        }
        return COUNTERS.isBlocked(target.getId())
                || COUNTERS.shouldBlock(
                target.getId(),
                COUNTERS.currentDepth(),
                DamageGuardConfig.getMaxDepth(),
                DamageGuardConfig.getMaxHitsPerTargetPerTick()
        );
    }

    private static void logLeakedDepth(int leaked) {
        long now = System.currentTimeMillis();
        if (now - lastLeakLogMs < DamageGuardConfig.getLogCooldownSeconds() * 1000L) {
            return;
        }
        lastLeakLogMs = now;
        LOGGER.warn("Damage guard: hurt() depth {} was still open at tick start (skipped exit); reset to 0", leaked);
    }

    private static void trip(LivingEntity target, DamageSource source, int depth, int hits) {
        Entity attacker = source == null ? null : source.getEntity();
        int attackerId = attacker == null ? 0 : attacker.getId();
        long key = ((long) attackerId << 32) | (target.getId() & 0xffff_ffffL);
        long now = System.currentTimeMillis();
        long cooldown = DamageGuardConfig.getLogCooldownSeconds() * 1000L;
        if (now - LAST_LOG_MS.get(key) < cooldown) {
            return;
        }
        LAST_LOG_MS.put(key, now);

        String attackerName = attacker instanceof Player player
                ? player.getGameProfile().getName()
                : attacker == null ? "unknown" : BuiltInRegistries.ENTITY_TYPE.getKey(attacker.getType()).toString();
        String weaponId = "";
        if (attacker instanceof Player player) {
            ItemStack stack = player.getMainHandItem();
            if (!stack.isEmpty()) {
                weaponId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            }
        }
        String targetType = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        String mods = DamageStackSummary.of(Thread.currentThread().getStackTrace());
        String line = String.format(
                Locale.ROOT,
                "Damage guard tripped: attacker=%s weapon=%s target=%s pos=%d,%d,%d depth=%d hits=%d mods=%s",
                attackerName,
                weaponId,
                targetType,
                target.getBlockX(),
                target.getBlockY(),
                target.getBlockZ(),
                depth,
                hits,
                mods
        );
        LOGGER.warn(line);
        if (!DamageGuardConfig.isNotifyAdmins()) {
            return;
        }
        MinecraftServer server = target.level() instanceof ServerLevel level ? level.getServer() : null;
        if (server == null) {
            return;
        }
        Component message = Component.literal("§eCointCore: " + line);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (PermissionService.has(player, CointPermissionNodes.WATCHDOG)) {
                player.sendSystemMessage(message);
            }
        }
    }

}
