package com.mawlee.cointcore.questrank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory skip list so a sweep does not {@code loadUser} the same player for the
 * same team and completed-quest set. Never a reason to remove LuckPerms groups.
 */
public final class QuestRankGrantCache {
    public static final int FULL_PASS_INTERVAL_SECONDS = 300;

    private final ConcurrentHashMap<CacheKey, Boolean> done = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UUID> teamByPlayer = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> completedHashByTeam = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<UUID>> membersByTeam = new ConcurrentHashMap<>();

    public record CacheKey(UUID playerId, UUID teamId, int completedHash) {
        public CacheKey {
            playerId = Objects.requireNonNull(playerId);
            teamId = Objects.requireNonNull(teamId);
        }
    }

    public boolean alreadyGranted(UUID playerId, UUID teamId, int completedHash) {
        if (playerId == null || teamId == null) {
            return false;
        }
        return done.containsKey(new CacheKey(playerId, teamId, completedHash));
    }

    public void markGranted(UUID playerId, UUID teamId, int completedHash) {
        if (playerId == null || teamId == null) {
            return;
        }
        done.put(new CacheKey(playerId, teamId, completedHash), Boolean.TRUE);
    }

    /**
     * @return previous team id when this player moved to a different team
     */
    public Optional<UUID> trackTeam(UUID playerId, UUID teamId) {
        if (playerId == null || teamId == null) {
            return Optional.empty();
        }
        UUID previous = teamByPlayer.put(playerId, teamId);
        if (previous == null || previous.equals(teamId)) {
            return Optional.empty();
        }
        return Optional.of(previous);
    }

    /**
     * Records the team's member set and completed-quest hash.
     *
     * @return {@code true} if either changed since the last record (cache for this team is cleared)
     */
    public boolean updateTeamState(UUID teamId, Set<UUID> members, int completedHash) {
        if (teamId == null) {
            return false;
        }
        Set<UUID> snapshot = members == null ? Set.of() : Set.copyOf(members);
        Integer previousHash = completedHashByTeam.put(teamId, completedHash);
        Set<UUID> previousMembers = membersByTeam.put(teamId, snapshot);
        boolean hashChanged = previousHash != null && previousHash != completedHash;
        boolean membersChanged = previousMembers != null && !previousMembers.equals(snapshot);
        if (hashChanged || membersChanged) {
            invalidateTeam(teamId);
            return true;
        }
        return false;
    }

    public void invalidateTeam(UUID teamId) {
        if (teamId == null) {
            return;
        }
        done.keySet().removeIf(key -> teamId.equals(key.teamId()));
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
        teamByPlayer.clear();
        completedHashByTeam.clear();
        membersByTeam.clear();
    }
}
