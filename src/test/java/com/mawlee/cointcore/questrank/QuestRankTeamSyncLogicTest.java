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
    void teamJoinWinsOverOnlineAndOffline() {
        assertEquals(
                QuestRankTeamSyncLogic.REASON_TEAM_JOIN,
                QuestRankTeamSyncLogic.grantReason(true, true, QuestRankTeamSyncLogic.REASON_SWEEP)
        );
        assertEquals(
                QuestRankTeamSyncLogic.REASON_TEAM_JOIN,
                QuestRankTeamSyncLogic.grantReason(true, false, QuestRankTeamSyncLogic.REASON_SWEEP)
        );
    }

    @Test
    void sweepUsesOfflineReasonForOfflineMembers() {
        assertEquals(
                QuestRankTeamSyncLogic.REASON_SWEEP,
                QuestRankTeamSyncLogic.grantReason(false, true, QuestRankTeamSyncLogic.REASON_SWEEP)
        );
        assertEquals(
                QuestRankTeamSyncLogic.REASON_OFFLINE,
                QuestRankTeamSyncLogic.grantReason(false, false, QuestRankTeamSyncLogic.REASON_SWEEP)
        );
    }

    @Test
    void loginAndCommandReasonsStayExplicit() {
        assertEquals(
                QuestRankTeamSyncLogic.REASON_LOGIN,
                QuestRankTeamSyncLogic.grantReason(false, true, QuestRankTeamSyncLogic.REASON_LOGIN)
        );
        assertEquals(
                QuestRankTeamSyncLogic.REASON_COMMAND,
                QuestRankTeamSyncLogic.grantReason(false, false, QuestRankTeamSyncLogic.REASON_COMMAND)
        );
    }

    @Test
    void skipsLuckPermsWhenCacheHitOrNothingCompleted() {
        assertFalse(QuestRankTeamSyncLogic.needsLuckPermsLoad(true, Set.of(EXPLORER), TABLE));
        assertFalse(QuestRankTeamSyncLogic.needsLuckPermsLoad(false, Set.of(), TABLE));
        assertFalse(QuestRankTeamSyncLogic.needsLuckPermsLoad(false, Set.of(1L), TABLE));
        assertTrue(QuestRankTeamSyncLogic.needsLuckPermsLoad(false, Set.of(EXPLORER), TABLE));
    }

    @Test
    void backoffOnlyOnPeriodicPaths() {
        assertTrue(QuestRankTeamSyncLogic.shouldBackoff(QuestRankTeamSyncLogic.REASON_SWEEP));
        assertTrue(QuestRankTeamSyncLogic.shouldBackoff(QuestRankTeamSyncLogic.REASON_OFFLINE));
        assertFalse(QuestRankTeamSyncLogic.shouldBackoff(QuestRankTeamSyncLogic.REASON_TEAM_JOIN));
        assertFalse(QuestRankTeamSyncLogic.shouldBackoff(QuestRankTeamSyncLogic.REASON_LOGIN));
        assertFalse(QuestRankTeamSyncLogic.shouldBackoff(QuestRankTeamSyncLogic.REASON_COMMAND));
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
