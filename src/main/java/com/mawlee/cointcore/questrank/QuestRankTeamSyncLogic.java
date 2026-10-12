package com.mawlee.cointcore.questrank;

import java.util.Set;

/**
 * Pure decisions for team-wide quest-rank grants. Never implies group removal.
 */
public final class QuestRankTeamSyncLogic {
    public static final String REASON_SWEEP = "sweep";
    public static final String REASON_TEAM_JOIN = "team_join";
    public static final String REASON_OFFLINE = "offline";
    public static final String REASON_LOGIN = "login";
    public static final String REASON_COMMAND = "command";

    private QuestRankTeamSyncLogic() {
    }

    /**
     * Team-join wins, then an explicit caller reason, then online sweep vs offline load.
     */
    public static String grantReason(boolean teamChanged, boolean online, String callerReason) {
        if (teamChanged) {
            return REASON_TEAM_JOIN;
        }
        if (REASON_LOGIN.equals(callerReason)
                || REASON_COMMAND.equals(callerReason)
                || REASON_TEAM_JOIN.equals(callerReason)) {
            return callerReason;
        }
        return online ? REASON_SWEEP : REASON_OFFLINE;
    }

    /**
     * Skip LuckPerms {@code loadUser} when this player/team/completed-set was already ensured,
     * or when there is nothing mapped and completed to grant.
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
        return REASON_SWEEP.equals(reason) || REASON_OFFLINE.equals(reason);
    }
}
