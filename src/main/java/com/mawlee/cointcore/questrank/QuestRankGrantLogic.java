package com.mawlee.cointcore.questrank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure grant decision: every mapped group whose quest is complete and that the player
 * does not already hold (direct or inherited). Never removes groups.
 */
public final class QuestRankGrantLogic {
    private QuestRankGrantLogic() {
    }

    public static List<String> groupsToGrant(
            Collection<Long> completedQuestIds,
            Collection<String> heldGroups,
            QuestRankTable table
    ) {
        if (table == null || table.isEmpty() || completedQuestIds == null || completedQuestIds.isEmpty()) {
            return List.of();
        }
        Set<Long> completed = completedQuestIds instanceof Set<Long> set
                ? set
                : Set.copyOf(completedQuestIds);
        Set<String> held = normalizeHeld(heldGroups);
        List<String> grants = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (var entry : table.questToGroup().entrySet()) {
            if (!completed.contains(entry.getKey())) {
                continue;
            }
            String group = entry.getValue();
            if (group == null || held.contains(group) || !seen.add(group)) {
                continue;
            }
            grants.add(group);
        }
        return List.copyOf(grants);
    }

    public static Set<String> normalizeHeld(Collection<String> heldGroups) {
        if (heldGroups == null || heldGroups.isEmpty()) {
            return Set.of();
        }
        Set<String> held = new LinkedHashSet<>();
        for (String group : heldGroups) {
            String normalized = QuestRankTable.normalizeGroup(group);
            if (normalized != null) {
                held.add(normalized);
            }
        }
        return held;
    }

    public static boolean alreadyHeld(Collection<String> heldGroups, String group) {
        String normalized = QuestRankTable.normalizeGroup(group);
        return normalized != null && normalizeHeld(heldGroups).contains(normalized);
    }
}
