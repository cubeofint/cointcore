package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.config.ChunkLimitConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limited cable-bus parts (AE2 storage buses and the like) per loaded chunk, for the same chunk / team limits as
 * blocks: a part counts under the key its item id resolves to ({@link ChunkLimitConfig#resolvePartKey}). Within a
 * chunk the first parts in (pos, side) order fill what blocks of the same key left of the limit; the rest are
 * inert (storage buses stop mounting and scanning).
 *
 * <p>Parts change without block updates, so hooks only {@link #markDirty} the chunk and {@link #tick} rescans it,
 * syncs the team totals and tells the parts whose active state flipped.
 */
public final class PartLimitIndex {
    /** One limited part in the world. {@code side} is the Direction ordinal, 6 for the cable centre. */
    public record PartRef(long pos, int side) implements Comparable<PartRef> {
        @Override
        public int compareTo(PartRef other) {
            int byPos = BlockPos.of(pos).compareTo(BlockPos.of(other.pos));
            return byPos != 0 ? byPos : Integer.compare(side, other.side);
        }
    }

    public record FoundPart(BlockPos pos, int side, ResourceLocation itemId) {
    }

    /** Implemented by the AE2 integration; absent without AE2. */
    public interface Scanner {
        List<FoundPart> scan(LevelChunk chunk);

        /** The part's active state changed: remount / wake it. */
        void onActivityChanged(ServerLevel level, BlockPos pos, int side);
    }

    private record ChunkRef(ResourceKey<Level> dimension, long chunk) {
    }

    private static final Map<ResourceKey<Level>, Map<Long, Bucket>> BY_DIMENSION = new ConcurrentHashMap<>();
    private static final Set<ChunkRef> DIRTY = ConcurrentHashMap.newKeySet();
    private static volatile Scanner scanner;

    private PartLimitIndex() {
    }

    public static void setScanner(Scanner value) {
        scanner = value;
    }

    public static boolean available() {
        return scanner != null;
    }

    public static void markDirty(ServerLevel level, BlockPos pos) {
        if (scanner != null) {
            DIRTY.add(new ChunkRef(level.dimension(), ChunkPos.asLong(pos)));
        }
    }

    public static void invalidateAll() {
        BY_DIMENSION.clear();
    }

    public static void removeChunk(ServerLevel level, ChunkPos chunkPos) {
        Map<Long, Bucket> dim = BY_DIMENSION.get(level.dimension());
        if (dim != null) {
            dim.remove(chunkPos.toLong());
        }
    }

    /** Called from {@link ChunkLimitIndex#rebuildChunk} so block and part counts are rebuilt together. */
    static void rescan(ServerLevel level, LevelChunk chunk) {
        Scanner current = scanner;
        if (current == null) {
            return;
        }
        Bucket bucket = new Bucket();
        for (FoundPart part : current.scan(chunk)) {
            ChunkLimitKey chunkKey = ChunkLimitConfig.resolvePartKey(ChunkLimitConfig.LimitScope.CHUNK, part.itemId());
            ChunkLimitKey teamKey = ChunkLimitConfig.resolvePartKey(ChunkLimitConfig.LimitScope.TEAM, part.itemId());
            PartRef ref = new PartRef(part.pos().asLong(), part.side());
            if (chunkKey != null) {
                bucket.add(chunkKey.id(), ref);
                bucket.chunkKeys.put(ref, chunkKey);
            }
            if (teamKey != null && (chunkKey == null || !Objects.equals(chunkKey.id(), teamKey.id()))) {
                bucket.add(teamKey.id(), ref);
            }
        }
        Map<Long, Bucket> dim = BY_DIMENSION.computeIfAbsent(level.dimension(), ignored -> new ConcurrentHashMap<>());
        Bucket previous = dim.put(chunk.getPos().toLong(), bucket);
        bucket.inactive = bucket.computeInactive(level, chunk.getPos());
        notifyChanges(level, previous == null ? Set.of() : previous.inactive, bucket.inactive);
    }

    /** Rescans chunks whose parts changed; runs once per server tick. */
    public static void tick(MinecraftServer server) {
        if (DIRTY.isEmpty()) {
            return;
        }
        List<ChunkRef> work = new ArrayList<>(DIRTY);
        DIRTY.removeAll(work);
        for (ChunkRef ref : work) {
            ServerLevel level = server.getLevel(ref.dimension());
            if (level == null) {
                continue;
            }
            ChunkPos chunkPos = new ChunkPos(ref.chunk());
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                continue;
            }
            if (!ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.hasAnyBlockLimits()) {
                continue;
            }
            rescan(level, level.getChunk(chunkPos.x, chunkPos.z));
            TeamLimitIndex.syncChunk(level, chunkPos);
        }
    }

    public static int count(ServerLevel level, ChunkPos chunkPos, String keyId) {
        Bucket bucket = bucket(level, chunkPos);
        return bucket == null ? 0 : bucket.count(keyId);
    }

    static Map<String, Integer> teamKeyCounts(ServerLevel level, ChunkPos chunkPos) {
        Bucket bucket = bucket(level, chunkPos);
        return bucket == null ? Map.of() : bucket.countsMatching(ChunkLimitConfig::isTeamKeyId);
    }

    /** Whether this part is within its chunk limit; parts without a chunk limit are always active. */
    public static boolean isActive(ServerLevel level, BlockPos pos, int side) {
        Bucket bucket = bucket(level, new ChunkPos(pos));
        return bucket == null || !bucket.inactive.contains(new PartRef(pos.asLong(), side));
    }

    private static Bucket bucket(ServerLevel level, ChunkPos chunkPos) {
        Map<Long, Bucket> dim = BY_DIMENSION.get(level.dimension());
        return dim == null ? null : dim.get(chunkPos.toLong());
    }

    private static void notifyChanges(ServerLevel level, Set<PartRef> before, Set<PartRef> after) {
        Scanner current = scanner;
        if (current == null || before.equals(after)) {
            return;
        }
        Set<PartRef> changed = new HashSet<>(before);
        changed.addAll(after);
        Set<PartRef> both = new HashSet<>(before);
        both.retainAll(after);
        changed.removeAll(both);
        for (PartRef ref : changed) {
            current.onActivityChanged(level, BlockPos.of(ref.pos()), ref.side());
        }
    }

    private static final class Bucket {
        private final Map<String, NavigableSet<PartRef>> parts = new HashMap<>();
        private final Map<PartRef, ChunkLimitKey> chunkKeys = new HashMap<>();
        private volatile Set<PartRef> inactive = Set.of();

        private void add(String keyId, PartRef ref) {
            parts.computeIfAbsent(keyId, ignored -> new TreeSet<>()).add(ref);
        }

        private int count(String keyId) {
            NavigableSet<PartRef> set = parts.get(keyId);
            return set == null ? 0 : set.size();
        }

        private Map<String, Integer> countsMatching(java.util.function.Predicate<String> predicate) {
            Map<String, Integer> result = new LinkedHashMap<>();
            parts.forEach((key, set) -> {
                if (predicate.test(key)) {
                    result.put(key, set.size());
                }
            });
            return result;
        }

        /** Parts past their chunk limit, after the blocks of the same key took their share. */
        private Set<PartRef> computeInactive(ServerLevel level, ChunkPos chunkPos) {
            Set<PartRef> result = new HashSet<>();
            Map<String, ChunkLimitKey> keys = new HashMap<>();
            chunkKeys.values().forEach(key -> keys.put(key.id(), key));
            for (ChunkLimitKey key : keys.values()) {
                NavigableSet<PartRef> set = parts.get(key.id());
                if (set == null) {
                    continue;
                }
                int free = Math.max(0, key.limit() - ChunkLimitIndex.blockCount(level, chunkPos, key));
                int rank = 0;
                for (PartRef ref : set) {
                    if (rank++ >= free) {
                        result.add(ref);
                    }
                }
            }
            return Set.copyOf(result);
        }
    }
}
