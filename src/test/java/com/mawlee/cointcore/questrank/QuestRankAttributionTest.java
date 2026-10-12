package com.mawlee.cointcore.questrank;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRankAttributionTest {
    private static final UUID PLAYER_A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID PLAYER_B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    @Test
    void currentPlayerWinsOverTeamLists() {
        Optional<UUID> credited = QuestRankAttribution.completingPlayer(
                PLAYER_A,
                List.of(PLAYER_A, PLAYER_B),
                List.of(PLAYER_A, PLAYER_B)
        );
        assertEquals(PLAYER_A, credited.orElseThrow());
    }

    @Test
    void teamListsAreNotUsedWhenSeveralMembersArePresent() {
        assertTrue(QuestRankAttribution.completingPlayer(
                null,
                List.of(PLAYER_A, PLAYER_B),
                List.of(PLAYER_A, PLAYER_B)
        ).isEmpty());
    }

    @Test
    void singleNotifiedPlayerIsUsedWhenContextIsMissing() {
        assertEquals(
                PLAYER_B,
                QuestRankAttribution.completingPlayer(null, List.of(PLAYER_B), List.of(PLAYER_A, PLAYER_B))
                        .orElseThrow()
        );
    }

    @Test
    void singleOnlineMemberIsLastResort() {
        assertEquals(
                PLAYER_A,
                QuestRankAttribution.completingPlayer(null, List.of(), List.of(PLAYER_A)).orElseThrow()
        );
        assertTrue(QuestRankAttribution.completingPlayer(null, List.of(), List.of()).isEmpty());
        assertTrue(QuestRankAttribution.completingPlayer(null, null, null).isEmpty());
    }
}
