package com.mawlee.cointcore.questrank;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * After a grant write that still leaves the group missing, skip that player/group
 * on later sweeps and warn once.
 */
public final class QuestRankGrantBackoff {
    private static final ConcurrentHashMap<String, Boolean> FAILED = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> WARNED = new ConcurrentHashMap<>();

    private QuestRankGrantBackoff() {
    }

    public static List<String> filter(UUID playerId, List<String> groups) {
        if (groups == null || groups.isEmpty()) {
            return groups == null ? List.of() : groups;
        }
        if (playerId == null) {
            return List.copyOf(groups);
        }
        List<String> allowed = new ArrayList<>(groups.size());
        for (String group : groups) {
            if (!FAILED.containsKey(key(playerId, group))) {
                allowed.add(group);
            }
        }
        return List.copyOf(allowed);
    }

    /**
     * @return {@code true} the first time this player/group is marked, so the caller can WARN once
     */
    public static boolean markMissingAfterWrite(UUID playerId, String group) {
        String mapKey = key(playerId, group);
        if (mapKey == null) {
            return false;
        }
        FAILED.put(mapKey, Boolean.TRUE);
        return WARNED.putIfAbsent(mapKey, Boolean.TRUE) == null;
    }

    public static void clear(UUID playerId, String group) {
        String mapKey = key(playerId, group);
        if (mapKey == null) {
            return;
        }
        FAILED.remove(mapKey);
        WARNED.remove(mapKey);
    }

    public static boolean isBackedOff(UUID playerId, String group) {
        String mapKey = key(playerId, group);
        return mapKey != null && FAILED.containsKey(mapKey);
    }

    public static void reset() {
        FAILED.clear();
        WARNED.clear();
    }

    private static String key(UUID playerId, String group) {
        if (playerId == null) {
            return null;
        }
        String name = QuestRankTable.normalizeGroup(group);
        return name == null ? null : playerId + ":" + name;
    }
}
