package com.mawlee.cointcore.questrank;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRankGrantLogicTest {
    private static final QuestRankTable TABLE = QuestRankTable.defaults();
    private static final long EXPLORER = QuestRankIds.parseHex("3FABD06F3F6D3BC3").orElseThrow();
    private static final long TITAN = QuestRankIds.parseHex("6491B39FBC0C53B2").orElseThrow();
    private static final long JESUS = QuestRankIds.parseHex("22E4E7DE8AF39F1D").orElseThrow();

    @Test
    void grantsEveryCompletedMappedGroupThePlayerDoesNotHold() {
        List<String> grants = QuestRankGrantLogic.groupsToGrant(
                Set.of(EXPLORER, TITAN, JESUS),
                Set.of("explorer", "helper"),
                TABLE
        );
        assertEquals(List.of("titan", "jesus"), grants);
    }

    @Test
    void inheritedAndCaseVariantsCountAsAlreadyHeld() {
        List<String> grants = QuestRankGrantLogic.groupsToGrant(
                Set.of(TITAN, JESUS),
                List.of(" Titan ", "JESUS"),
                TABLE
        );
        assertTrue(grants.isEmpty());
        assertTrue(QuestRankGrantLogic.alreadyHeld(Set.of("Titan"), "titan"));
        assertFalse(QuestRankGrantLogic.alreadyHeld(Set.of("titan"), "jesus"));
    }

    @Test
    void unfinishedAndUnknownQuestsDoNotGrant() {
        List<String> grants = QuestRankGrantLogic.groupsToGrant(
                Set.of(1L, EXPLORER),
                Set.of(),
                TABLE
        );
        assertEquals(List.of("explorer"), grants);
    }

    @Test
    void neverImpliesRemovalWhenPlayerHasExtraGroups() {
        List<String> grants = QuestRankGrantLogic.groupsToGrant(
                Set.of(EXPLORER),
                Set.of("explorer", "titan", "admin"),
                TABLE
        );
        assertTrue(grants.isEmpty());
        assertFalse(grants.contains("titan"));
    }

    @Test
    void emptyInputsGrantNothing() {
        assertTrue(QuestRankGrantLogic.groupsToGrant(Set.of(), Set.of(), TABLE).isEmpty());
        assertTrue(QuestRankGrantLogic.groupsToGrant(Set.of(EXPLORER), Set.of(), new QuestRankTable(java.util.Map.of())).isEmpty());
        assertTrue(QuestRankGrantLogic.groupsToGrant(null, Set.of("titan"), TABLE).isEmpty());
    }
}
