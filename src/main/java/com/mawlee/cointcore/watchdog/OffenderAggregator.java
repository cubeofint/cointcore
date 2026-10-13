package com.mawlee.cointcore.watchdog;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Aggregates nanoseconds spent by block entities, entities, chunks and dimensions.
 *
 * <p>Hot path stores interned object keys and primitive totals only: no String building,
 * no boxing, no lambdas. Registry ids and team owners are resolved at {@link #top} time.
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
        private final Object type;
        private final Object dimension;
        private final int x;
        private final int y;
        private final int z;

        private MutableStat(Kind kind, Object type, Object dimension, int x, int y, int z) {
            this.kind = kind;
            this.type = type;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private void add(long nanos) {
            totalNanos += nanos;
            ticks++;
        }

        private OffenderSnapshot snapshot(WatchdogLabels labels) {
            String typeId;
            String dimensionId = labels.dimensionId(dimension);
            if (kind == Kind.CHUNK) {
                typeId = "chunk";
            } else if (kind == Kind.DIMENSION) {
                typeId = dimensionId;
            } else {
                typeId = labels.typeId(type);
            }
            String modId = kind == Kind.DIMENSION ? "minecraft" : modFromType(typeId);
            double totalMillis = totalNanos / 1_000_000.0D;
            double averageMillis = ticks == 0L ? 0.0D : totalMillis / ticks;
            return new OffenderSnapshot(
                    kind,
                    typeId,
                    modId,
                    dimensionId,
                    x,
                    y,
                    z,
                    x >> 4,
                    z >> 4,
                    totalNanos,
                    ticks,
                    averageMillis,
                    totalMillis
            );
        }
    }

    private static final class InstanceKey {
        private Kind kind;
        private Object type;
        private Object dimension;
        private int x;
        private int y;
        private int z;

        private void set(Kind kind, Object type, Object dimension, int x, int y, int z) {
            this.kind = kind;
            this.type = type;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private InstanceKey copy() {
            InstanceKey key = new InstanceKey();
            key.set(kind, type, dimension, x, y, z);
            return key;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof InstanceKey key)) {
                return false;
            }
            return kind == key.kind
                    && x == key.x
                    && y == key.y
                    && z == key.z
                    && type == key.type
                    && dimension == key.dimension;
        }

        @Override
        public int hashCode() {
            int hash = kind == null ? 0 : kind.ordinal();
            hash = 31 * hash + System.identityHashCode(type);
            hash = 31 * hash + System.identityHashCode(dimension);
            hash = 31 * hash + x;
            hash = 31 * hash + y;
            hash = 31 * hash + z;
            return hash;
        }
    }

    private static final class ChunkKey {
        private Object dimension;
        private int chunkX;
        private int chunkZ;

        private void set(Object dimension, int chunkX, int chunkZ) {
            this.dimension = dimension;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        private ChunkKey copy() {
            ChunkKey key = new ChunkKey();
            key.set(dimension, chunkX, chunkZ);
            return key;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ChunkKey key)) {
                return false;
            }
            return chunkX == key.chunkX && chunkZ == key.chunkZ && dimension == key.dimension;
        }

        @Override
        public int hashCode() {
            int hash = System.identityHashCode(dimension);
            hash = 31 * hash + chunkX;
            hash = 31 * hash + chunkZ;
            return hash;
        }
    }

    private final Object2ObjectOpenHashMap<InstanceKey, MutableStat> instances = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<ChunkKey, MutableStat> chunks = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<Object, MutableStat> dimensions = new Object2ObjectOpenHashMap<>();
    private final InstanceKey instanceLookup = new InstanceKey();
    private final ChunkKey chunkLookup = new ChunkKey();
    private final ArrayList<OffenderSnapshot> topBuffer = new ArrayList<>();
    private WatchdogLabels labels = WatchdogLabels.STRINGS;

    public void setLabels(WatchdogLabels labels) {
        this.labels = labels == null ? WatchdogLabels.STRINGS : labels;
    }

    public void addBlockEntity(String typeId, String modId, String dimension, int x, int y, int z, long nanos) {
        add(Kind.BLOCK_ENTITY, internString(typeId), internString(dimension), x, y, z, nanos);
    }

    public void addEntity(String typeId, String modId, String dimension, int x, int y, int z, long nanos) {
        add(Kind.ENTITY, internString(typeId), internString(dimension), x, y, z, nanos);
    }

    public void addBlockEntity(Object type, Object dimension, int x, int y, int z, long nanos) {
        add(Kind.BLOCK_ENTITY, type, dimension, x, y, z, nanos);
    }

    public void addEntity(Object type, Object dimension, int x, int y, int z, long nanos) {
        add(Kind.ENTITY, type, dimension, x, y, z, nanos);
    }

    public List<OffenderSnapshot> top(Kind kind, int limit) {
        int capped = Math.max(0, limit);
        topBuffer.clear();
        if (kind == null || kind == Kind.BLOCK_ENTITY || kind == Kind.ENTITY) {
            for (MutableStat stat : instances.values()) {
                if (kind == null || stat.kind == kind) {
                    topBuffer.add(stat.snapshot(labels));
                }
            }
        }
        if (kind == null || kind == Kind.CHUNK) {
            for (MutableStat stat : chunks.values()) {
                topBuffer.add(stat.snapshot(labels));
            }
        }
        if (kind == null || kind == Kind.DIMENSION) {
            for (MutableStat stat : dimensions.values()) {
                topBuffer.add(stat.snapshot(labels));
            }
        }
        topBuffer.sort(BY_NANOS_THEN_TYPE);
        if (topBuffer.size() > capped) {
            return List.copyOf(topBuffer.subList(0, capped));
        }
        return List.copyOf(topBuffer);
    }

    public List<OffenderSnapshot> topAll(int limit) {
        return top(null, limit);
    }

    public void clear() {
        instances.clear();
        chunks.clear();
        dimensions.clear();
        topBuffer.clear();
    }

    public int size() {
        return instances.size() + chunks.size() + dimensions.size();
    }

    private void add(Kind kind, Object type, Object dimension, int x, int y, int z, long nanos) {
        if (nanos <= 0L) {
            return;
        }
        Object typeKey = type instanceof String string ? internString(string) : type;
        Object dimKey = dimension instanceof String string
                ? internString(string)
                : (dimension == null ? "unknown" : dimension);
        instanceLookup.set(kind, typeKey, dimKey, x, y, z);
        MutableStat stat = instances.get(instanceLookup);
        if (stat == null) {
            InstanceKey key = instanceLookup.copy();
            stat = new MutableStat(kind, typeKey, dimKey, x, y, z);
            instances.put(key, stat);
        }
        stat.add(nanos);

        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        chunkLookup.set(dimKey, chunkX, chunkZ);
        MutableStat chunk = chunks.get(chunkLookup);
        if (chunk == null) {
            ChunkKey key = chunkLookup.copy();
            chunk = new MutableStat(Kind.CHUNK, "chunk", dimKey, chunkX << 4, 0, chunkZ << 4);
            chunks.put(key, chunk);
        }
        chunk.add(nanos);

        MutableStat dim = dimensions.get(dimKey);
        if (dim == null) {
            dim = new MutableStat(Kind.DIMENSION, dimKey, dimKey, 0, 0, 0);
            dimensions.put(dimKey, dim);
        }
        dim.add(nanos);
    }

    private static String internString(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().intern();
    }

    private static String modFromType(String typeId) {
        int colon = typeId.indexOf(':');
        if (colon > 0) {
            return typeId.substring(0, colon).toLowerCase(Locale.ROOT);
        }
        return "unknown";
    }

    private static final Comparator<OffenderSnapshot> BY_NANOS_THEN_TYPE =
            Comparator.comparingLong(OffenderSnapshot::totalNanos).reversed()
                    .thenComparing(OffenderSnapshot::typeId);
}
