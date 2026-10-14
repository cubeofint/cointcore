package com.mawlee.cointcore.chunklimit;

import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mawlee.cointcore.ftb.FtbIntegration;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
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

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int RESCAN_PER_TICK = 4;
    private static final Deque<ChunkRef> RESCAN_QUEUE = new ArrayDeque<>();
    private static volatile TeamLimitSavedData savedData;

    @FunctionalInterface
    public interface ContributionVisitor {
        void accept(String dimension, long chunk, UUID teamId, Map<String, Integer> counts);
    }

    private TeamLimitIndex() {
    }

    /** Full reset (server stop). Config reloads keep persisted contributions and rescan loaded chunks instead. */
    public static void invalidateAll() {
        BY_CHUNK.clear();
        BY_TEAM.clear();
        synchronized (RESCAN_QUEUE) {
            RESCAN_QUEUE.clear();
        }
        savedData = null;
    }

    /** Lazily attach persisted contributions; in-memory (fresher) entries win over disk. */
    private static void ensureLoaded(MinecraftServer server) {
        if (savedData != null || server == null) {
            return;
        }
        synchronized (TeamLimitIndex.class) {
            if (savedData != null) {
                return;
            }
            TeamLimitSavedData data;
            try {
                data = TeamLimitSavedData.get(server);
            } catch (RuntimeException exception) {
                LOGGER.warn("Team limit data not available yet", exception);
                return;
            }
            int restored = 0;
            for (TeamLimitSavedData.Entry entry : data.drainLoaded()) {
                ResourceLocation dimId = ResourceLocation.tryParse(entry.dimension());
                if (dimId == null) {
                    continue;
                }
                ChunkRef ref = new ChunkRef(ResourceKey.create(Registries.DIMENSION, dimId), entry.chunk());
                if (BY_CHUNK.putIfAbsent(ref, new Contribution(entry.teamId(), Map.copyOf(entry.counts()))) == null) {
                    add(entry.teamId(), entry.counts());
                    restored++;
                }
            }
            savedData = data;
            LOGGER.info("Restored {} persisted team block-limit chunk contributions", restored);
        }
    }

    private static void ensureLoaded() {
        ensureLoaded(ServerLifecycleHooks.getCurrentServer());
    }

    private static void markDirty() {
        TeamLimitSavedData data = savedData;
        if (data != null) {
            data.setDirty();
        }
    }

    public static void forEachContribution(ContributionVisitor visitor) {
        for (Map.Entry<ChunkRef, Contribution> entry : BY_CHUNK.entrySet()) {
            visitor.accept(
                    entry.getKey().dimension().location().toString(),
                    entry.getKey().chunkLong(),
                    entry.getValue().teamId(),
                    entry.getValue().counts()
            );
        }
    }

    /** After a config change: recount loaded chunks a few per tick (no full-world scan, no tick spike). */
    public static void scheduleRescan(Map<ResourceKey<Level>, List<Long>> loadedChunks) {
        synchronized (RESCAN_QUEUE) {
            loadedChunks.forEach((dim, chunks) -> chunks.forEach(c -> RESCAN_QUEUE.add(new ChunkRef(dim, c))));
        }
    }

    public static void tickRescan(MinecraftServer server) {
        for (int i = 0; i < RESCAN_PER_TICK; i++) {
            ChunkRef ref;
            synchronized (RESCAN_QUEUE) {
                ref = RESCAN_QUEUE.poll();
            }
            if (ref == null) {
                return;
            }
            ServerLevel level = server.getLevel(ref.dimension());
            if (level == null) {
                continue;
            }
            ChunkPos pos = new ChunkPos(ref.chunkLong());
            if (level.hasChunk(pos.x, pos.z)) {
                ChunkLimitIndex.ensureIndexed(level, pos);
            }
        }
    }

    /**
     * Replace this chunk's contribution using current team-key counts from {@link ChunkLimitIndex}.
     * No-op if FTB is unavailable or the chunk is unclaimed (clears prior contribution).
     */
    public static void syncChunk(ServerLevel level, ChunkPos chunkPos) {
        ensureLoaded(level.getServer());
        if (!ChunkLimitConfig.isEnabled() || !ChunkLimitConfig.hasTeamBlockLimits()) {
            clearChunk(level.dimension(), chunkPos.toLong());
            return;
        }

        UUID teamId = FtbIntegration.getTeamIdAt(level, chunkPos).orElse(null);
        if (teamId == null) {
            clearChunk(level.dimension(), chunkPos.toLong());
            return;
        }

        ChunkRef ref = new ChunkRef(level.dimension(), chunkPos.toLong());
        Map<String, Integer> counts;
        if (level.hasChunk(chunkPos.x, chunkPos.z)) {
            counts = ChunkLimitIndex.teamKeyCounts(level, chunkPos);
        } else {
            Contribution existing = BY_CHUNK.get(ref);
            if (existing == null) {
                return;
            }
            counts = existing.counts();
        }
        Contribution previous = BY_CHUNK.put(ref, new Contribution(teamId, Map.copyOf(counts)));
        if (previous != null) {
            subtract(previous.teamId(), previous.counts());
        }
        add(teamId, counts);
        markDirty();
    }

    /** Drop contribution (e.g. unclaim). Does not run on normal chunk unload. */
    public static void clearChunk(ServerLevel level, ChunkPos chunkPos) {
        clearChunk(level.dimension(), chunkPos.toLong());
    }

    private static void clearChunk(ResourceKey<Level> dimension, long chunkLong) {
        ensureLoaded();
        Contribution previous = BY_CHUNK.remove(new ChunkRef(dimension, chunkLong));
        if (previous != null) {
            subtract(previous.teamId(), previous.counts());
            markDirty();
        }
    }

    public static int count(UUID teamId, ChunkLimitKey key) {
        if (teamId == null || key == null) {
            return 0;
        }
        ensureLoaded();
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
