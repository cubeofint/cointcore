package com.mawlee.cointcore.questrank;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player completed rank-quest ids. Team FTB progress is never stored here.
 */
public final class QuestRankCompletionStore {
    private final Map<UUID, Set<Long>> completedByPlayer = new HashMap<>();

    public synchronized boolean record(UUID playerId, long questId) {
        if (playerId == null) {
            return false;
        }
        return completedByPlayer.computeIfAbsent(playerId, ignored -> new LinkedHashSet<>()).add(questId);
    }

    /** Drops these quest ids from the player's record; returns how many were there. */
    public synchronized int forget(UUID playerId, Set<Long> questIds) {
        Set<Long> completed = playerId == null ? null : completedByPlayer.get(playerId);
        if (completed == null || questIds == null) {
            return 0;
        }
        int removed = 0;
        for (Long questId : questIds) {
            if (completed.remove(questId)) {
                removed++;
            }
        }
        if (completed.isEmpty()) {
            completedByPlayer.remove(playerId);
        }
        return removed;
    }

    public synchronized boolean has(UUID playerId, long questId) {
        if (playerId == null) {
            return false;
        }
        Set<Long> completed = completedByPlayer.get(playerId);
        return completed != null && completed.contains(questId);
    }

    public synchronized Set<Long> completed(UUID playerId) {
        if (playerId == null) {
            return Set.of();
        }
        Set<Long> completed = completedByPlayer.get(playerId);
        if (completed == null || completed.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(completed);
    }

    public synchronized Map<UUID, Set<Long>> snapshot() {
        Map<UUID, Set<Long>> copy = new LinkedHashMap<>();
        for (Map.Entry<UUID, Set<Long>> entry : completedByPlayer.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            copy.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return copy;
    }

    synchronized void replaceAll(Map<UUID, Set<Long>> loaded) {
        completedByPlayer.clear();
        if (loaded == null) {
            return;
        }
        for (Map.Entry<UUID, Set<Long>> entry : loaded.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            completedByPlayer.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        }
    }
}
