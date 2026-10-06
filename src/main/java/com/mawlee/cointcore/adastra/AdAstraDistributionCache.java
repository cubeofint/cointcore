package com.mawlee.cointcore.adastra;

import earth.terrarium.adastra.common.config.MachineConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Skips a distributor or normalizer flood while every sealed block and its
 * orthogonal wall neighbors are unchanged. A real scan is spread across the
 * refresh window by block position.
 */
public final class AdAstraDistributionCache {
    private static final int INDEX_LIMIT = 100_000;

    private static final WeakHashMap<BlockEntity, Volume> VOLUMES = new WeakHashMap<>();
    private static final WeakHashMap<BlockEntity, Boolean> ARMED = new WeakHashMap<>();
    private static final WeakHashMap<ServerLevel, Long2ObjectOpenHashMap<List<Volume>>> INDEX = new WeakHashMap<>();

    private AdAstraDistributionCache() {
    }

    public static boolean isStable(BlockEntity machine) {
        Volume volume = VOLUMES.get(machine);
        return volume != null && !volume.dirty;
    }

    public static void arm(BlockEntity machine) {
        ARMED.put(machine, Boolean.TRUE);
    }

    public static void disarm(BlockEntity machine) {
        ARMED.remove(machine);
    }

    public static boolean isDue(BlockEntity machine, BlockPos pos, long gameTime) {
        return ARMED.containsKey(machine) && Math.floorMod(gameTime, refreshRate()) == phase(pos);
    }

    public static int phase(BlockPos pos) {
        return Math.floorMod(pos.asLong(), refreshRate());
    }

    public static void snapshot(BlockEntity machine, ServerLevel level, Set<BlockPos> filled) {
        Volume previous = VOLUMES.remove(machine);
        if (previous != null) {
            unindex(previous);
        }
        disarm(machine);
        if (filled.isEmpty()) {
            return;
        }
        LongOpenHashSet positions = new LongOpenHashSet(filled.size() * 2);
        for (BlockPos pos : filled) {
            addWatched(positions, pos.asLong());
        }
        Volume volume = new Volume(level, positions);
        VOLUMES.put(machine, volume);
        index(volume);
    }

    public static void invalidate(BlockEntity machine) {
        disarm(machine);
        Volume volume = VOLUMES.remove(machine);
        if (volume != null) {
            unindex(volume);
        }
    }

    public static void mark(ServerLevel level, BlockPos pos) {
        Long2ObjectOpenHashMap<List<Volume>> index = INDEX.get(level);
        if (index == null) {
            return;
        }
        List<Volume> volumes = index.get(pos.asLong());
        if (volumes == null) {
            return;
        }
        for (int i = 0; i < volumes.size(); i++) {
            volumes.get(i).dirty = true;
        }
    }

    private static int refreshRate() {
        int rate = MachineConfig.distributionRefreshRate;
        return rate <= 0 ? 100 : rate;
    }

    private static void addWatched(LongOpenHashSet positions, long packed) {
        positions.add(packed);
        positions.add(BlockPos.offset(packed, 1, 0, 0));
        positions.add(BlockPos.offset(packed, -1, 0, 0));
        positions.add(BlockPos.offset(packed, 0, 1, 0));
        positions.add(BlockPos.offset(packed, 0, -1, 0));
        positions.add(BlockPos.offset(packed, 0, 0, 1));
        positions.add(BlockPos.offset(packed, 0, 0, -1));
    }

    private static void index(Volume volume) {
        Long2ObjectOpenHashMap<List<Volume>> index = INDEX.computeIfAbsent(volume.level, ignored -> new Long2ObjectOpenHashMap<>());
        if (index.size() > INDEX_LIMIT) {
            dropLevel(volume.level);
            index = INDEX.computeIfAbsent(volume.level, ignored -> new Long2ObjectOpenHashMap<>());
        }
        for (long packed : volume.positions) {
            index.computeIfAbsent(packed, ignored -> new ArrayList<>(1)).add(volume);
        }
    }

    private static void unindex(Volume volume) {
        Long2ObjectOpenHashMap<List<Volume>> index = INDEX.get(volume.level);
        if (index == null) {
            return;
        }
        for (long packed : volume.positions) {
            List<Volume> volumes = index.get(packed);
            if (volumes == null) {
                continue;
            }
            volumes.remove(volume);
            if (volumes.isEmpty()) {
                index.remove(packed);
            }
        }
    }

    private static void dropLevel(ServerLevel level) {
        INDEX.remove(level);
        VOLUMES.entrySet().removeIf(entry -> entry.getValue().level == level);
    }

    private static final class Volume {
        private final ServerLevel level;
        private final LongOpenHashSet positions;
        private boolean dirty;

        private Volume(ServerLevel level, LongOpenHashSet positions) {
            this.level = level;
            this.positions = positions;
        }
    }
}
