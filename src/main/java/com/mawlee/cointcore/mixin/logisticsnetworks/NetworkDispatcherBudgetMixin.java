package com.mawlee.cointcore.mixin.logisticsnetworks;

import com.mawlee.cointcore.config.LogisticsNetworksBudgetConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Soft mixin: only applied when {@code logisticsnetworks} is loaded. Caps the time its synchronous dispatch may take
 * per tick ({@link LogisticsNetworksBudgetConfig}); networks past the budget are re-queued for the next tick, and one
 * deferred last tick always runs this tick. Logs single networks that are slow on their own.
 *
 * <p>Verified against logisticsnetworks-1.21.1-1.17.2 ({@code NetworkDispatcher#processSynchronously}).
 */
@Pseudo
@Mixin(targets = "me.almana.logisticsnetworks.data.NetworkDispatcher", remap = false)
public abstract class NetworkDispatcherBudgetMixin {
    @Unique
    private static final Logger COINTCORE$LOGGER = LogUtils.getLogger();
    @Unique
    private static final long COINTCORE$SLOW_LOG_INTERVAL_MS = 60_000L;

    @Unique
    private long cointcore$tick = Long.MIN_VALUE;
    @Unique
    private long cointcore$tickStartNanos;
    @Unique
    private long cointcore$networkStartNanos;
    @Unique
    private Set<UUID> cointcore$deferredLastTick = new HashSet<>();
    @Unique
    private Set<UUID> cointcore$deferredThisTick = new HashSet<>();
    @Unique
    private final Map<UUID, Long> cointcore$slowLoggedAt = new HashMap<>();

    @Shadow
    abstract void markDirty(UUID networkId);

    @Inject(method = "processSynchronously", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$budgetHead(UUID networkId, @Coerce Object network, MinecraftServer server, long gameTime,
                                      CallbackInfo ci) {
        long now = System.nanoTime();
        if (gameTime != cointcore$tick) {
            cointcore$tick = gameTime;
            cointcore$tickStartNanos = now;
            Set<UUID> swap = cointcore$deferredLastTick;
            cointcore$deferredLastTick = cointcore$deferredThisTick;
            cointcore$deferredThisTick = swap;
            cointcore$deferredThisTick.clear();
        }
        if (LogisticsNetworksBudgetConfig.isEnabled()
                && now - cointcore$tickStartNanos > LogisticsNetworksBudgetConfig.budgetNanos()
                && !cointcore$deferredLastTick.contains(networkId)) {
            cointcore$deferredThisTick.add(networkId);
            markDirty(networkId);
            ci.cancel();
            return;
        }
        cointcore$networkStartNanos = now;
    }

    @Inject(method = "processSynchronously", at = @At("RETURN"), remap = false)
    private void cointcore$budgetReturn(UUID networkId, @Coerce Object network, MinecraftServer server, long gameTime,
                                        CallbackInfo ci) {
        long took = System.nanoTime() - cointcore$networkStartNanos;
        if (took < LogisticsNetworksBudgetConfig.slowNetworkNanos()) {
            return;
        }
        long nowMs = System.currentTimeMillis();
        Long last = cointcore$slowLoggedAt.get(networkId);
        if (last != null && nowMs - last < COINTCORE$SLOW_LOG_INTERVAL_MS) {
            return;
        }
        cointcore$slowLoggedAt.put(networkId, nowMs);
        COINTCORE$LOGGER.warn("Slow Logistics Network {} took {} ms in one tick: {}", networkId,
                took / 1_000_000L, cointcore$describe(network, server));
    }

    @Unique
    private static String cointcore$describe(Object network, MinecraftServer server) {
        try {
            Object name = network.getClass().getMethod("getName").invoke(network);
            Object owner = network.getClass().getMethod("getOwnerUuid").invoke(network);
            Object nodes = network.getClass().getMethod("getNodeUuids").invoke(network);
            String ownerName = owner instanceof UUID uuid
                    ? com.mawlee.cointcore.mute.MuteService.resolveName(server, uuid).orElse(uuid.toString())
                    : "?";
            int nodeCount = nodes instanceof java.util.Collection<?> collection ? collection.size() : -1;
            return "name='" + name + "', owner=" + ownerName + ", nodes=" + nodeCount;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return network.toString();
        }
    }
}
