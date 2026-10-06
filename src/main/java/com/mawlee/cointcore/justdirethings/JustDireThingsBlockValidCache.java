package com.mawlee.cointcore.justdirethings;

import com.direwolf20.justdirethings.common.blockentities.BlockBreakerT2BE;
import com.direwolf20.justdirethings.common.containers.handlers.FilterBasicHandler;
import com.direwolf20.justdirethings.util.interfacehelpers.FilterData;
import com.mawlee.cointcore.config.JustDireThingsPerfConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Caches {@link BlockBreakerT2BE#isBlockValid} results so drop-filter mode does not
 * re-run {@code Block.getDrops} for unchanged positions between area rescans.
 */
public final class JustDireThingsBlockValidCache {
    private static final IdentityHashMap<BlockBreakerT2BE, Map<Long, Entry>> CACHE = new IdentityHashMap<>();
    private static final ThreadLocal<Boolean> RETURNED_FROM_CACHE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private JustDireThingsBlockValidCache() {
    }

    public static boolean tryReturnCached(
            BlockBreakerT2BE be,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        RETURNED_FROM_CACHE.set(Boolean.FALSE);
        if (!JustDireThingsPerfConfig.isEnabled()) {
            return false;
        }
        int ttl = JustDireThingsPerfConfig.resolveBlockValidCacheTicks();
        if (ttl <= 0) {
            return false;
        }
        Level level = be.getLevel();
        if (level == null || level.isClientSide()) {
            return false;
        }
        Map<Long, Entry> perBe = CACHE.get(be);
        if (perBe == null) {
            return false;
        }
        Entry entry = perBe.get(pos.asLong());
        if (entry == null) {
            return false;
        }
        long now = level.getGameTime();
        if (now - entry.cachedAtGameTime >= ttl) {
            perBe.remove(pos.asLong());
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (entry.state != state) {
            perBe.remove(pos.asLong());
            return false;
        }
        if (entry.fingerprint != fingerprint(be)) {
            perBe.clear();
            return false;
        }
        RETURNED_FROM_CACHE.set(Boolean.TRUE);
        cir.setReturnValue(entry.valid);
        return true;
    }

    public static void store(BlockBreakerT2BE be, BlockPos pos, Boolean valid) {
        try {
            if (Boolean.TRUE.equals(RETURNED_FROM_CACHE.get())) {
                return;
            }
            if (valid == null || !JustDireThingsPerfConfig.isEnabled()) {
                return;
            }
            int ttl = JustDireThingsPerfConfig.resolveBlockValidCacheTicks();
            if (ttl <= 0) {
                return;
            }
            Level level = be.getLevel();
            if (level == null || level.isClientSide()) {
                return;
            }
            Map<Long, Entry> perBe = CACHE.computeIfAbsent(be, ignored -> new HashMap<>());
            perBe.put(
                    pos.asLong(),
                    new Entry(valid, level.getBlockState(pos), fingerprint(be), level.getGameTime())
            );
        } finally {
            RETURNED_FROM_CACHE.remove();
        }
    }

    public static void invalidate(BlockBreakerT2BE be) {
        CACHE.remove(be);
    }

    public static void clearAll() {
        CACHE.clear();
    }

    private static int fingerprint(BlockBreakerT2BE be) {
        FilterData filterData = be.getFilterData();
        int hash = 17;
        hash = 31 * hash + (filterData.allowlist ? 1 : 0);
        hash = 31 * hash + (filterData.compareNBT ? 1 : 0);
        hash = 31 * hash + filterData.blockItemFilter;
        FilterBasicHandler filter = be.getFilterHandler();
        if (filter != null) {
            int slots = filter.getSlots();
            hash = 31 * hash + slots;
            for (int i = 0; i < slots; i++) {
                ItemStack stack = filter.getStackInSlot(i);
                hash = 31 * hash + stack.getItem().hashCode();
                hash = 31 * hash + stack.getCount();
                if (filterData.compareNBT && !stack.isEmpty()) {
                    hash = 31 * hash + stack.getComponentsPatch().hashCode();
                }
            }
        }
        ItemStack tool = be.getTool();
        hash = 31 * hash + tool.getItem().hashCode();
        hash = 31 * hash + tool.getCount();
        if (!tool.isEmpty()) {
            hash = 31 * hash + tool.getComponentsPatch().hashCode();
        }
        return hash;
    }

    private record Entry(boolean valid, BlockState state, int fingerprint, long cachedAtGameTime) {
    }
}
