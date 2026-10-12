package com.mawlee.cointcore.questrank;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRankGrantBackoffTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000cc");

    @AfterEach
    void tearDown() {
        QuestRankGrantBackoff.reset();
    }

    @Test
    void warnsOnceAndSkipsLaterSweepsForMissingGroup() {
        List<String> missing = List.of("atm", "titan");
        assertEquals(missing, QuestRankGrantBackoff.filter(PLAYER, missing));

        assertTrue(QuestRankGrantBackoff.markMissingAfterWrite(PLAYER, "atm"));
        assertFalse(QuestRankGrantBackoff.markMissingAfterWrite(PLAYER, "atm"));
        assertTrue(QuestRankGrantBackoff.isBackedOff(PLAYER, "ATM"));

        assertEquals(List.of("titan"), QuestRankGrantBackoff.filter(PLAYER, missing));
        assertEquals(List.of(), QuestRankGrantBackoff.filter(PLAYER, List.of("atm")));
    }

    @Test
    void successfulGrantClearsBackoff() {
        QuestRankGrantBackoff.markMissingAfterWrite(PLAYER, "atm");
        QuestRankGrantBackoff.clear(PLAYER, "atm");
        assertFalse(QuestRankGrantBackoff.isBackedOff(PLAYER, "atm"));
        assertEquals(List.of("atm"), QuestRankGrantBackoff.filter(PLAYER, List.of("atm")));
    }

    @Test
    void emptyMissingListDoesNotNeedModifyUser() {
        assertEquals(List.of(), QuestRankGrantBackoff.filter(PLAYER, List.of()));
        assertEquals(List.of(), QuestRankGrantLogic.groupsToGrant(
                java.util.Set.of(),
                java.util.Set.of("atm"),
                QuestRankTable.defaults()
        ));
    }
}
