package com.mawlee.cointcore.watchdog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Aggregates nanoseconds spent by block entities, entities, chunks and dimensions.
 */
public final class OffenderAggregator {
    public enum Kind {
        BLOCK_ENTITY,
        ENTITY,
        CHUNK,
        DIMENSION
    }

    public record OffenderSnapshot(
            Kind kind,
            String typeId,
            String modId,
            String dimension,
            int x,
            int y,
            int z,
            int chunkX,
            int chunkZ,
            long totalNanos,
            long ticks,
            double averageMillis,
            double totalMillis
    ) {
    }

    private static final class MutableStat {
        private long totalNanos;
        private long ticks;
        private final Kind kind;
        private final String typeId;
        private final String modId;
        private final String dimension;
        private final int x;
        private final int y;
        private final int z;
        private final int chunkX;
        private final int chunkZ;

        private MutableStat(
                Kind kind,
                String typeId,
                String modId,
                String dimension,
                int x,
                int y,
                int z,
                int chunkX,
                int chunkZ
        ) {
            this.kind = kind;
            this.typeId = typeId;
            this.modId = modId;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        private void add(long nanos) {
            totalNanos += nanos;
            ticks++;
        }

        private OffenderSnapshot snapshot() {
            double totalMillis = totalNanos / 1_000_000.0D;
            double averageMillis = ticks == 0L ? 0.0D : totalMillis / ticks;
            return new OffenderSnapshot(
                    kind,
                    typeId,
                    modId,
                    dimension,
                    x,
                    y,
                    z,
                    chunkX,
                    chunkZ,
                    totalNanos,
                    ticks,
                    averageMillis,
                    totalMillis
            );
        }
    }

    private final Map<String, MutableStat> stats = new HashMap<>();

    public void addBlockEntity(String typeId, String modId, String dimension, int x, int y, int z, long nanos) {
        if (nanos <= 0L) {
            return;
        }
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        String safeType = safe(typeId, "unknown");
        String safeMod = safe(modId, modFromType(safeType));
        String safeDim = safe(dimension, "unknown");
        String key = "be|" + safeDim + '|' + x + '|' + y + '|' + z + '|' + safeType;
        stats.computeIfAbsent(key, ignored -> new MutableStat(
                Kind.BLOCK_ENTITY, safeType, safeMod, safeDim, x, y, z, chunkX, chunkZ
        )).add(nanos);

        String chunkKey = "chunk|" + safeDim + '|' + chunkX + '|' + chunkZ;
        stats.computeIfAbsent(chunkKey, ignored -> new MutableStat(
                Kind.CHUNK, "chunk", safeMod, safeDim, chunkX << 4, 0, chunkZ << 4, chunkX, chunkZ
        )).add(nanos);

        String dimKey = "dim|" + safeDim;
        stats.computeIfAbsent(dimKey, ignored -> new MutableStat(
                Kind.DIMENSION, safeDim, "minecraft", safeDim, 0, 0, 0, 0, 0
        )).add(nanos);
    }

    public void addEntity(String typeId, String modId, String dimension, int x, int y, int z, long nanos) {
        if (nanos <= 0L) {
            return;
        }
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        String safeType = safe(typeId, "unknown");
        String safeMod = safe(modId, modFromType(safeType));
        String safeDim = safe(dimension, "unknown");
        String key = "ent|" + safeDim + '|' + x + '|' + y + '|' + z + '|' + safeType;
        stats.computeIfAbsent(key, ignored -> new MutableStat(
                Kind.ENTITY, safeType, safeMod, safeDim, x, y, z, chunkX, chunkZ
        )).add(nanos);

        String chunkKey = "chunk|" + safeDim + '|' + chunkX + '|' + chunkZ;
        stats.computeIfAbsent(chunkKey, ignored -> new MutableStat(
                Kind.CHUNK, "chunk", safeMod, safeDim, chunkX << 4, 0, chunkZ << 4, chunkX, chunkZ
        )).add(nanos);

        String dimKey = "dim|" + safeDim;
        stats.computeIfAbsent(dimKey, ignored -> new MutableStat(
                Kind.DIMENSION, safeDim, "minecraft", safeDim, 0, 0, 0, 0, 0
        )).add(nanos);
    }

    public List<OffenderSnapshot> top(Kind kind, int limit) {
        int capped = Math.max(0, limit);
        List<OffenderSnapshot> list = new ArrayList<>();
        for (MutableStat stat : stats.values()) {
            if (kind == null || stat.kind == kind) {
                list.add(stat.snapshot());
            }
        }
        list.sort(Comparator.comparingLong(OffenderSnapshot::totalNanos).reversed()
                .thenComparing(OffenderSnapshot::typeId));
        if (list.size() > capped) {
            return List.copyOf(list.subList(0, capped));
        }
        return List.copyOf(list);
    }

    public List<OffenderSnapshot> topAll(int limit) {
        return top(null, limit);
    }

    public void clear() {
        stats.clear();
    }

    public int size() {
        return stats.size();
    }

    private static String safe(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String modFromType(String typeId) {
        int colon = typeId.indexOf(':');
        if (colon > 0) {
            return typeId.substring(0, colon).toLowerCase(Locale.ROOT);
        }
        return "unknown";
    }
}
