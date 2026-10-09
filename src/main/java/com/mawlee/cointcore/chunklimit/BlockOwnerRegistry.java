package com.mawlee.cointcore.chunklimit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Owner of each player-placed limited block plus per-owner counts per player-scope limit key.
 * Counts are derived from stored block ids through {@code keyResolver}, so a config change only
 * needs {@link #ensureResolver} with a new version instead of touching unloaded chunks.
 */
public final class BlockOwnerRegistry {
    public record Entry(UUID owner, String blockId) {
    }

    @FunctionalInterface
    public interface EntryVisitor {
        void visit(String dimension, long pos, Entry entry);
    }

    private final Map<String, Map<Long, Entry>> byDimension = new HashMap<>();
    private final Map<String, Map<Long, Set<Long>>> positionsByChunk = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> counts = new HashMap<>();
    private Function<String, String> keyResolver = blockId -> null;
    private int resolverVersion = Integer.MIN_VALUE;
    private int size;

    public synchronized void ensureResolver(int version, Function<String, String> resolver) {
        if (version == resolverVersion) {
            return;
        }
        resolverVersion = version;
        keyResolver = resolver;
        counts.clear();
        for (Map<Long, Entry> entries : byDimension.values()) {
            for (Entry entry : entries.values()) {
                adjust(entry, 1);
            }
        }
    }

    public synchronized Entry put(String dimension, long chunk, long pos, UUID owner, String blockId) {
        Entry entry = new Entry(owner, blockId);
        Entry previous = byDimension.computeIfAbsent(dimension, ignored -> new HashMap<>()).put(pos, entry);
        if (previous != null) {
            adjust(previous, -1);
        } else {
            size++;
            positionsByChunk
                    .computeIfAbsent(dimension, ignored -> new HashMap<>())
                    .computeIfAbsent(chunk, ignored -> new HashSet<>())
                    .add(pos);
        }
        adjust(entry, 1);
        return previous;
    }

    public synchronized Entry remove(String dimension, long chunk, long pos) {
        Map<Long, Entry> entries = byDimension.get(dimension);
        if (entries == null) {
            return null;
        }
        Entry removed = entries.remove(pos);
        if (removed == null) {
            return null;
        }
        size--;
        if (entries.isEmpty()) {
            byDimension.remove(dimension);
        }
        Map<Long, Set<Long>> chunks = positionsByChunk.get(dimension);
        if (chunks != null) {
            Set<Long> positions = chunks.get(chunk);
            if (positions != null) {
                positions.remove(pos);
                if (positions.isEmpty()) {
                    chunks.remove(chunk);
                }
            }
            if (chunks.isEmpty()) {
                positionsByChunk.remove(dimension);
            }
        }
        adjust(removed, -1);
        return removed;
    }

    public synchronized Entry get(String dimension, long pos) {
        Map<Long, Entry> entries = byDimension.get(dimension);
        return entries == null ? null : entries.get(pos);
    }

    public synchronized List<Long> positionsInChunk(String dimension, long chunk) {
        Map<Long, Set<Long>> chunks = positionsByChunk.get(dimension);
        if (chunks == null) {
            return List.of();
        }
        Set<Long> positions = chunks.get(chunk);
        return positions == null ? List.of() : new ArrayList<>(positions);
    }

    public synchronized int count(UUID owner, String keyId) {
        Map<String, Integer> ownerCounts = counts.get(owner);
        return ownerCounts == null ? 0 : ownerCounts.getOrDefault(keyId, 0);
    }

    public synchronized boolean isEmpty() {
        return size == 0;
    }

    public synchronized int size() {
        return size;
    }

    public synchronized void forEach(EntryVisitor visitor) {
        for (Map.Entry<String, Map<Long, Entry>> dimension : byDimension.entrySet()) {
            for (Map.Entry<Long, Entry> entry : dimension.getValue().entrySet()) {
                visitor.visit(dimension.getKey(), entry.getKey(), entry.getValue());
            }
        }
    }

    private void adjust(Entry entry, int delta) {
        String keyId = keyResolver.apply(entry.blockId());
        if (keyId == null) {
            return;
        }
        Map<String, Integer> ownerCounts = counts.computeIfAbsent(entry.owner(), ignored -> new HashMap<>());
        int next = ownerCounts.getOrDefault(keyId, 0) + delta;
        if (next > 0) {
            ownerCounts.put(keyId, next);
        } else {
            ownerCounts.remove(keyId);
            if (ownerCounts.isEmpty()) {
                counts.remove(entry.owner());
            }
        }
    }
}
