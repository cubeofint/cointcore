package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.config.ChunkLimitConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-chunk sorted positions of limited blocks. First {@code limit} positions (BlockPos order) are active
 * for chunk-scoped keys. Team-scoped keys are indexed for {@link TeamLimitIndex} aggregation.
 */
public final class ChunkLimitIndex {
    private static final Map<ResourceKey<Level>, Map<Long, ChunkBucket>> BY_DIMENSION = new ConcurrentHashMap<>();

    private ChunkLimitIndex() {
    }

    public static void invalidateAll() {
        BY_DIMENSION.clear();
    }

    public static void removeChunk(ServerLevel level, ChunkPos chunkPos) {
        Map<Long, ChunkBucket> dim = BY_DIMENSION.get(level.dimension());
        if (dim != null) {
            dim.remove(chunkPos.toLong());
        }
        // Keep TeamLimitIndex contribution across unload.
    }

    public static void rebuildChunk(ServerLevel level, LevelChunk chunk) {
        if (!ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.hasAnyBlockLimits()) {
            removeChunk(level, chunk.getPos());
            TeamLimitIndex.clearChunk(level, chunk.getPos());
            return;
        }

        ChunkPos chunkPos = chunk.getPos();
        ChunkBucket bucket = new ChunkBucket();
        int minY = level.getMinBuildHeight();

        LevelChunkSection[] sections = chunk.getSections();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            int sectionBottomY = minY + sectionIndex * 16;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, y, z);
                        BlockPos pos = new BlockPos(
                                chunkPos.getMinBlockX() + x,
                                sectionBottomY + y,
                                chunkPos.getMinBlockZ() + z
                        );
                        indexBlock(bucket, state.getBlock(), pos);
                    }
                }
            }
        }

        BY_DIMENSION
                .computeIfAbsent(level.dimension(), ignored -> new ConcurrentHashMap<>())
                .put(chunkPos.toLong(), bucket);
        TeamLimitIndex.syncChunk(level, chunkPos);
    }

    public static void onBlockChange(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (!ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.hasAnyBlockLimits()) {
            return;
        }
        if (oldState.getBlock() == newState.getBlock()) {
            return;
        }
        PlayerBlockLimitService.onBlockChange(level, pos, oldState, newState);

        ChunkLimitKey oldChunkKey = ChunkLimitConfig.resolveBlockKey(oldState.getBlock());
        ChunkLimitKey newChunkKey = ChunkLimitConfig.resolveBlockKey(newState.getBlock());
        ChunkLimitKey oldTeamKey = ChunkLimitConfig.resolveTeamBlockKey(oldState.getBlock());
        ChunkLimitKey newTeamKey = ChunkLimitConfig.resolveTeamBlockKey(newState.getBlock());
        if (oldChunkKey == null && newChunkKey == null && oldTeamKey == null && newTeamKey == null) {
            return;
        }

        ChunkPos chunkPos = new ChunkPos(pos);
        ChunkBucket bucket = bucketFor(level, chunkPos, true);
        removeKeys(bucket, pos, oldChunkKey, oldTeamKey);
        if (!newState.isAir()) {
            addKeys(bucket, pos, newChunkKey, newTeamKey);
        }
        TeamLimitIndex.syncChunk(level, chunkPos);
    }

    public static int count(ServerLevel level, ChunkPos chunkPos, ChunkLimitKey key) {
        ChunkBucket bucket = bucketFor(level, chunkPos, false);
        if (bucket == null) {
            return 0;
        }
        return bucket.count(key.id());
    }

    /** Counts for team-scoped keys present in this chunk (for {@link TeamLimitIndex}). */
    public static Map<String, Integer> teamKeyCounts(ServerLevel level, ChunkPos chunkPos) {
        ChunkBucket bucket = bucketFor(level, chunkPos, false);
        if (bucket == null || !ChunkLimitConfig.hasTeamBlockLimits()) {
            return Map.of();
        }
        return bucket.countsMatching(ChunkLimitConfig::isTeamKeyId);
    }

    public static boolean isActive(ServerLevel level, BlockPos pos, Block block) {
        ChunkLimitKey key = ChunkLimitConfig.resolveBlockKey(block);
        if (key == null) {
            return true;
        }
        ChunkBucket bucket = bucketFor(level, new ChunkPos(pos), false);
        if (bucket == null) {
            return true;
        }
        return bucket.isActive(key.id(), pos, key.limit());
    }

    public static int inertCount(ServerLevel level, ChunkPos chunkPos, ChunkLimitKey key) {
        int total = count(level, chunkPos, key);
        return Math.max(0, total - key.limit());
    }

    private static void indexBlock(ChunkBucket bucket, Block block, BlockPos pos) {
        ChunkLimitKey chunkKey = ChunkLimitConfig.resolveBlockKey(block);
        ChunkLimitKey teamKey = ChunkLimitConfig.resolveTeamBlockKey(block);
        addKeys(bucket, pos, chunkKey, teamKey);
    }

    private static void addKeys(ChunkBucket bucket, BlockPos pos, ChunkLimitKey chunkKey, ChunkLimitKey teamKey) {
        if (chunkKey != null) {
            bucket.add(chunkKey, pos);
        }
        if (teamKey != null && (chunkKey == null || !Objects.equals(chunkKey.id(), teamKey.id()))) {
            bucket.add(teamKey, pos);
        } else if (teamKey != null && chunkKey != null && Objects.equals(chunkKey.id(), teamKey.id())) {
            // Same key id already added via chunk key.
        }
    }

    private static void removeKeys(ChunkBucket bucket, BlockPos pos, ChunkLimitKey chunkKey, ChunkLimitKey teamKey) {
        if (chunkKey != null) {
            bucket.remove(chunkKey, pos);
        }
        if (teamKey != null && (chunkKey == null || !Objects.equals(chunkKey.id(), teamKey.id()))) {
            bucket.remove(teamKey, pos);
        }
    }

    private static ChunkBucket bucketFor(ServerLevel level, ChunkPos chunkPos, boolean create) {
        Map<Long, ChunkBucket> dim = BY_DIMENSION.get(level.dimension());
        if (dim == null) {
            if (!create) {
                return null;
            }
            dim = BY_DIMENSION.computeIfAbsent(level.dimension(), ignored -> new ConcurrentHashMap<>());
        }
        ChunkBucket existing = dim.get(chunkPos.toLong());
        if (existing != null) {
            return existing;
        }
        if (level.hasChunk(chunkPos.x, chunkPos.z)) {
            // A partial bucket would undercount until the next reload; always start from a full scan.
            rebuildChunk(level, level.getChunk(chunkPos.x, chunkPos.z));
            ChunkBucket rebuilt = dim.get(chunkPos.toLong());
            if (rebuilt != null || !create) {
                return rebuilt;
            }
        }
        if (!create) {
            return null;
        }
        return dim.computeIfAbsent(chunkPos.toLong(), ignored -> new ChunkBucket());
    }

    private static final class ChunkBucket {
        private final Map<String, NavigableSet<BlockPos>> positions = new ConcurrentHashMap<>();
        private final Map<String, Map<BlockPos, Integer>> ranks = new ConcurrentHashMap<>();

        private synchronized void add(ChunkLimitKey key, BlockPos pos) {
            positions.computeIfAbsent(key.id(), ignored -> new TreeSet<>()).add(pos.immutable());
            ranks.remove(key.id());
        }

        private synchronized void remove(ChunkLimitKey key, BlockPos pos) {
            NavigableSet<BlockPos> set = positions.get(key.id());
            if (set == null) {
                return;
            }
            set.remove(pos);
            if (set.isEmpty()) {
                positions.remove(key.id());
                ranks.remove(key.id());
            } else {
                ranks.remove(key.id());
            }
        }

        private synchronized int count(String keyId) {
            NavigableSet<BlockPos> set = positions.get(keyId);
            return set == null ? 0 : set.size();
        }

        private synchronized Map<String, Integer> countsMatching(java.util.function.Predicate<String> predicate) {
            Map<String, Integer> result = new LinkedHashMap<>();
            for (Map.Entry<String, NavigableSet<BlockPos>> entry : positions.entrySet()) {
                if (!predicate.test(entry.getKey())) {
                    continue;
                }
                result.put(entry.getKey(), entry.getValue().size());
            }
            return result;
        }

        private synchronized boolean isActive(String keyId, BlockPos pos, int limit) {
            NavigableSet<BlockPos> set = positions.get(keyId);
            if (set == null || set.isEmpty()) {
                return true;
            }
            Map<BlockPos, Integer> rankMap = ranks.get(keyId);
            if (rankMap == null) {
                rankMap = rebuildRanks(keyId, set);
            }
            Integer rank = rankMap.get(pos);
            if (rank == null) {
                return true;
            }
            return rank < limit;
        }

        private Map<BlockPos, Integer> rebuildRanks(String keyId, NavigableSet<BlockPos> set) {
            Map<BlockPos, Integer> map = new HashMap<>(Math.max(16, set.size()));
            int rank = 0;
            for (BlockPos entry : set) {
                map.put(entry, rank++);
            }
            ranks.put(keyId, map);
            return map;
        }
    }
}
