package com.mawlee.cointcore.refinedstorage.multicrafter;

import com.mawlee.cointcore.config.MultiCrafterConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Validated structures per level, shared by all their blocks so a crafter is scanned once, not once per storage.
 * Any crafter block placed or removed in a level drops that level's cache.
 */
public final class MultiCrafterStructures {
    private static final Map<Level, Map<Long, MultiCrafterShape.Result>> CACHE = new WeakHashMap<>();
    private static final Map<Level, Integer> VERSIONS = new WeakHashMap<>();

    private MultiCrafterStructures() {
    }

    public static synchronized MultiCrafterShape.Result get(Level level, BlockPos pos) {
        Map<Long, MultiCrafterShape.Result> cache = CACHE.computeIfAbsent(level, ignored -> new HashMap<>());
        long key = MultiCrafterShape.pack(pos.getX(), pos.getY(), pos.getZ());
        MultiCrafterShape.Result cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        MultiCrafterShape.Result result = MultiCrafterShape.scan((x, y, z) -> {
            probe.set(x, y, z);
            if (!level.isLoaded(probe)) return null;
            return level.getBlockState(probe).getBlock() instanceof MultiCrafterBlock block ? block.part() : null;
        }, pos.getX(), pos.getY(), pos.getZ(), MultiCrafterConfig.getMaxSize());
        // Only valid structures are cached: an invalid one may just be waiting for a chunk to load.
        if (result.valid()) {
            for (long part : result.parts()) {
                cache.put(part, result);
            }
        }
        return result;
    }

    /** Bumped whenever the level's crafters may have changed; block entities re-check when it moves. */
    public static synchronized int version(Level level) {
        return VERSIONS.getOrDefault(level, 0);
    }

    public static synchronized void invalidate(Level level) {
        Map<Long, MultiCrafterShape.Result> cache = CACHE.get(level);
        if (cache != null) cache.clear();
        VERSIONS.merge(level, 1, Integer::sum);
    }
}
