package com.mawlee.cointcore.questrank;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRankTeamSyncLogicTest {
    private static final QuestRankTable TABLE = QuestRankTable.defaults();
    private static final long EXPLORER = QuestRankIds.parseHex("3FABD06F3F6D3BC3").orElseThrow();

    @Test
    void loginCommandAndCompleteStayExplicit() {
        assertEquals(
                QuestRankTeamSyncLogic.REASON_LOGIN,
                QuestRankTeamSyncLogic.grantReason(QuestRankTeamSyncLogic.REASON_LOGIN)
        );
        assertEquals(
                QuestRankTeamSyncLogic.REASON_COMMAND,
                QuestRankTeamSyncLogic.grantReason(QuestRankTeamSyncLogic.REASON_COMMAND)
        );
        assertEquals(
                QuestRankTeamSyncLogic.REASON_QUEST_COMPLETE,
                QuestRankTeamSyncLogic.grantReason(QuestRankTeamSyncLogic.REASON_QUEST_COMPLETE)
        );
        assertEquals(
                QuestRankTeamSyncLogic.REASON_SWEEP,
                QuestRankTeamSyncLogic.grantReason("other")
        );
    }

    @Test
    void skipsLuckPermsWhenCacheHitOrNothingRecorded() {
        assertFalse(QuestRankTeamSyncLogic.needsLuckPermsLoad(true, Set.of(EXPLORER), TABLE));
        assertFalse(QuestRankTeamSyncLogic.needsLuckPermsLoad(false, Set.of(), TABLE));
        assertFalse(QuestRankTeamSyncLogic.needsLuckPermsLoad(false, Set.of(1L), TABLE));
        assertTrue(QuestRankTeamSyncLogic.needsLuckPermsLoad(false, Set.of(EXPLORER), TABLE));
    }

    @Test
    void backoffOnlyOnPeriodicSweep() {
        assertTrue(QuestRankTeamSyncLogic.shouldBackoff(QuestRankTeamSyncLogic.REASON_SWEEP));
        assertFalse(QuestRankTeamSyncLogic.shouldBackoff(QuestRankTeamSyncLogic.REASON_LOGIN));
        assertFalse(QuestRankTeamSyncLogic.shouldBackoff(QuestRankTeamSyncLogic.REASON_COMMAND));
        assertFalse(QuestRankTeamSyncLogic.shouldBackoff(QuestRankTeamSyncLogic.REASON_QUEST_COMPLETE));
    }

    @Test
    void grantLogicStillNeverRemovesGroups() {
        assertTrue(QuestRankGrantLogic.groupsToGrant(
                Set.of(EXPLORER),
                Set.of("explorer", "titan", "admin"),
                TABLE
        ).isEmpty());
    }
}
