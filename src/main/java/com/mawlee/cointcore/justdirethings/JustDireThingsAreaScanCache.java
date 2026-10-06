package com.mawlee.cointcore.justdirethings;

import com.mawlee.cointcore.config.JustDireThingsPerfConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Server-thread cache for Just Dire Things area {@code betweenClosed} scans.
 */
public final class JustDireThingsAreaScanCache {
    private static final IdentityHashMap<BlockEntity, Entry> CACHE = new IdentityHashMap<>();
    private static final ThreadLocal<Boolean> RETURNED_FROM_CACHE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private JustDireThingsAreaScanCache() {
    }

    public static boolean tryReturnCached(BlockEntity be, CallbackInfoReturnable<List<BlockPos>> cir) {
        RETURNED_FROM_CACHE.set(Boolean.FALSE);
        if (!JustDireThingsPerfConfig.isEnabled()) {
            return false;
        }
        int ttl = JustDireThingsPerfConfig.resolveAreaScanCacheTicks();
        if (ttl <= 0) {
            return false;
        }
        Level level = be.getLevel();
        if (level == null || level.isClientSide()) {
            return false;
        }
        Entry entry = CACHE.get(be);
        if (entry == null) {
            return false;
        }
        long now = level.getGameTime();
        if (now - entry.cachedAtGameTime >= ttl) {
            CACHE.remove(be);
            return false;
        }
        RETURNED_FROM_CACHE.set(Boolean.TRUE);
        cir.setReturnValue(new ArrayList<>(entry.positions));
        return true;
    }

    public static void store(BlockEntity be, List<BlockPos> positions) {
        try {
            if (Boolean.TRUE.equals(RETURNED_FROM_CACHE.get())) {
                return;
            }
            if (!JustDireThingsPerfConfig.isEnabled()) {
                return;
            }
            int ttl = JustDireThingsPerfConfig.resolveAreaScanCacheTicks();
            if (ttl <= 0 || positions == null) {
                return;
            }
            Level level = be.getLevel();
            if (level == null || level.isClientSide()) {
                return;
            }
            CACHE.put(be, new Entry(List.copyOf(positions), level.getGameTime()));
        } finally {
            RETURNED_FROM_CACHE.remove();
        }
    }

    public static void invalidate(BlockEntity be) {
        CACHE.remove(be);
    }

    public static void clearAll() {
        CACHE.clear();
    }

    private record Entry(List<BlockPos> positions, long cachedAtGameTime) {
    }
}
