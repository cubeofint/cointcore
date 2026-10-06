package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mawlee.cointcore.ftb.FtbIntegration;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Team-wide mob counts for entity limit keys. Contributions survive chunk unload.
 */
public final class TeamMobLimitIndex {
    private record ChunkRef(ResourceKey<Level> dimension, long chunkLong) {
    }

    private record Contribution(UUID teamId, Map<String, Integer> counts) {
    }

    private static final Map<ChunkRef, Contribution> BY_CHUNK = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Integer>> BY_TEAM = new ConcurrentHashMap<>();

    private TeamMobLimitIndex() {
    }

    public static void invalidateAll() {
        BY_CHUNK.clear();
        BY_TEAM.clear();
    }

    public static void syncChunk(ServerLevel level, ChunkPos chunkPos, Map<String, Integer> counts) {
        if (!ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.hasTeamEntityLimits()) {
            clearChunk(level, chunkPos);
            return;
        }
        UUID teamId = FtbIntegration.getTeamIdAt(level, chunkPos).orElse(null);
        if (teamId == null) {
            clearChunk(level, chunkPos);
            return;
        }
        ChunkRef ref = new ChunkRef(level.dimension(), chunkPos.toLong());
        Contribution previous = BY_CHUNK.put(ref, new Contribution(teamId, Map.copyOf(counts)));
        if (previous != null) {
            subtract(previous.teamId(), previous.counts());
        }
        add(teamId, counts);
    }

    public static void clearChunk(ServerLevel level, ChunkPos chunkPos) {
        Contribution previous = BY_CHUNK.remove(new ChunkRef(level.dimension(), chunkPos.toLong()));
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
