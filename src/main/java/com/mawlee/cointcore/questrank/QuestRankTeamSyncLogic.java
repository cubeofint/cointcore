package com.mawlee.cointcore.questrank;

import java.util.Set;

/**
 * Pure decisions for per-player quest-rank grants. Never implies group removal.
 * Team-join and team-progress inference are not grant reasons.
 */
public final class QuestRankTeamSyncLogic {
    public static final String REASON_SWEEP = "sweep";
    public static final String REASON_LOGIN = "login";
    public static final String REASON_COMMAND = "command";
    public static final String REASON_QUEST_COMPLETE = "quest-complete";

    private QuestRankTeamSyncLogic() {
    }

    public static String grantReason(String callerReason) {
        if (REASON_LOGIN.equals(callerReason)
                || REASON_COMMAND.equals(callerReason)
                || REASON_QUEST_COMPLETE.equals(callerReason)) {
            return callerReason;
        }
        return REASON_SWEEP;
    }

    /**
     * Skip LuckPerms {@code loadUser} when this player/completed-set was already ensured,
     * or when there is nothing mapped and recorded to grant.
     */
    public static boolean needsLuckPermsLoad(
            boolean cacheHit,
            Set<Long> completedQuestIds,
            QuestRankTable table
    ) {
        if (cacheHit) {
            return false;
        }
        return !QuestRankGrantLogic.groupsToGrant(completedQuestIds, Set.of(), table).isEmpty();
    }

    public static boolean shouldBackoff(String reason) {
        return REASON_SWEEP.equals(reason);
    }
}
