package com.mawlee.cointcore.questrank;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRankCompletionStoreTest {
    private static final UUID PLAYER_A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID PLAYER_B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final long EXPLORER = QuestRankIds.parseHex("3FABD06F3F6D3BC3").orElseThrow();
    private static final long TITAN = QuestRankIds.parseHex("6491B39FBC0C53B2").orElseThrow();

    @Test
    void teammateDoesNotInheritRecordedCompletion() {
        QuestRankCompletionStore store = new QuestRankCompletionStore();
        assertTrue(store.record(PLAYER_A, EXPLORER));
        assertFalse(store.record(PLAYER_A, EXPLORER));
        assertTrue(store.has(PLAYER_A, EXPLORER));
        assertFalse(store.has(PLAYER_B, EXPLORER));
        assertEquals(Set.of(EXPLORER), store.completed(PLAYER_A));
        assertTrue(store.completed(PLAYER_B).isEmpty());
    }

    @Test
    void codecRoundTripKeepsPerPlayerQuests() {
        QuestRankCompletionStore store = new QuestRankCompletionStore();
        store.record(PLAYER_A, EXPLORER);
        store.record(PLAYER_A, TITAN);
        store.record(PLAYER_B, TITAN);

        Map<String, Object> saved = QuestRankStoreCodec.saveMap(store);
        QuestRankCompletionStore loaded = new QuestRankCompletionStore();
        QuestRankStoreCodec.loadMap(loaded, saved);

        assertEquals(Set.of(EXPLORER, TITAN), loaded.completed(PLAYER_A));
        assertEquals(Set.of(TITAN), loaded.completed(PLAYER_B));
        assertFalse(loaded.has(PLAYER_B, EXPLORER));
    }

    @Test
    void grantsUseRecordedIdsNotTeamProgress() {
        QuestRankTable table = QuestRankTable.defaults();
        QuestRankCompletionStore store = new QuestRankCompletionStore();
        store.record(PLAYER_A, EXPLORER);
        assertEquals(
                java.util.List.of("explorer"),
                QuestRankGrantLogic.groupsToGrant(store.completed(PLAYER_A), Set.of(), table)
        );
        assertTrue(QuestRankGrantLogic.groupsToGrant(store.completed(PLAYER_B), Set.of(), table).isEmpty());
        assertTrue(QuestRankGrantLogic.groupsToGrant(Set.of(EXPLORER, TITAN), Set.of("explorer", "titan"), table).isEmpty());
    }
}
