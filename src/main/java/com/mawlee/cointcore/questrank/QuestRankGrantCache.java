package com.mawlee.cointcore.questrank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory skip list so a sweep does not {@code loadUser} the same player for the
 * same recorded completed-quest set. Never a reason to remove LuckPerms groups.
 */
public final class QuestRankGrantCache {
    public static final int FULL_PASS_INTERVAL_SECONDS = 300;

    private final ConcurrentHashMap<CacheKey, Boolean> done = new ConcurrentHashMap<>();

    public record CacheKey(UUID playerId, int completedHash) {
        public CacheKey {
            playerId = Objects.requireNonNull(playerId);
        }
    }

    public boolean alreadyGranted(UUID playerId, int completedHash) {
        if (playerId == null) {
            return false;
        }
        return done.containsKey(new CacheKey(playerId, completedHash));
    }

    public void markGranted(UUID playerId, int completedHash) {
        if (playerId == null) {
            return;
        }
        done.put(new CacheKey(playerId, completedHash), Boolean.TRUE);
    }

    public void invalidatePlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }
        done.keySet().removeIf(key -> playerId.equals(key.playerId()));
    }

    public static int hashCompleted(Set<Long> completed) {
        if (completed == null || completed.isEmpty()) {
            return 0;
        }
        List<Long> sorted = new ArrayList<>(completed);
        Collections.sort(sorted);
        long hash = 1L;
        for (Long id : sorted) {
            hash = 31L * hash + Long.hashCode(id);
        }
        return Long.hashCode(hash);
    }

    public int size() {
        return done.size();
    }

    public void reset() {
        done.clear();
    }
}
