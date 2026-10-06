package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mawlee.cointcore.ftb.FtbIntegration;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aggregate counts of team-scoped limit keys across claimed chunks.
 * Contributions are retained across chunk unload so unloaded claims still count.
 */
public final class TeamLimitIndex {
    private record ChunkRef(ResourceKey<Level> dimension, long chunkLong) {
    }

    private record Contribution(UUID teamId, Map<String, Integer> counts) {
    }

    private static final Map<ChunkRef, Contribution> BY_CHUNK = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Integer>> BY_TEAM = new ConcurrentHashMap<>();

    private TeamLimitIndex() {
    }

    public static void invalidateAll() {
        BY_CHUNK.clear();
        BY_TEAM.clear();
    }

    /**
     * Replace this chunk's contribution using current team-key counts from {@link ChunkLimitIndex}.
     * No-op if FTB is unavailable or the chunk is unclaimed (clears prior contribution).
     */
    public static void syncChunk(ServerLevel level, ChunkPos chunkPos) {
        if (!ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.hasTeamBlockLimits()) {
            clearChunk(level.dimension(), chunkPos.toLong());
            return;
        }

        UUID teamId = FtbIntegration.getTeamIdAt(level, chunkPos).orElse(null);
        if (teamId == null) {
            clearChunk(level.dimension(), chunkPos.toLong());
            return;
        }

        Map<String, Integer> counts = ChunkLimitIndex.teamKeyCounts(level, chunkPos);
        ChunkRef ref = new ChunkRef(level.dimension(), chunkPos.toLong());
        Contribution previous = BY_CHUNK.put(ref, new Contribution(teamId, Map.copyOf(counts)));
        if (previous != null) {
            subtract(previous.teamId(), previous.counts());
        }
        add(teamId, counts);
    }

    /** Drop contribution (e.g. unclaim). Does not run on normal chunk unload. */
    public static void clearChunk(ServerLevel level, ChunkPos chunkPos) {
        clearChunk(level.dimension(), chunkPos.toLong());
    }

    private static void clearChunk(ResourceKey<Level> dimension, long chunkLong) {
        Contribution previous = BY_CHUNK.remove(new ChunkRef(dimension, chunkLong));
        if (previous != null) {
            subtract(previous.teamId(), previous.counts());
        }
    }

    public static int count(UUID teamId, ChunkLimitKey key) {
        if (teamId == null || key == null) {
            return 0;
        }
        Map<String, Integer> counts = BY_TEAM.get(teamId);
        if (counts == null) {
            return 0;
        }
        return counts.getOrDefault(key.id(), 0);
    }

    public static Map<String, Integer> snapshot(UUID teamId) {
        Map<String, Integer> counts = BY_TEAM.get(teamId);
        if (counts == null || counts.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(counts);
    }

    private static void add(UUID teamId, Map<String, Integer> counts) {
        if (counts.isEmpty()) {
            return;
        }
        Map<String, Integer> team = BY_TEAM.computeIfAbsent(teamId, ignored -> new ConcurrentHashMap<>());
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            team.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    private static void subtract(UUID teamId, Map<String, Integer> counts) {
        Map<String, Integer> team = BY_TEAM.get(teamId);
        if (team == null || counts.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            team.computeIfPresent(entry.getKey(), (key, value) -> {
                int next = value - entry.getValue();
                return next > 0 ? next : null;
            });
        }
        if (team.isEmpty()) {
            BY_TEAM.remove(teamId, team);
        }
    }
}
