package com.mawlee.cointcore.questrank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRankGrantCacheTest {
    private static final UUID PLAYER_A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID PLAYER_B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final long EXPLORER = QuestRankIds.parseHex("3FABD06F3F6D3BC3").orElseThrow();
    private static final long TITAN = QuestRankIds.parseHex("6491B39FBC0C53B2").orElseThrow();

    private QuestRankGrantCache cache;

    @BeforeEach
    void setUp() {
        cache = new QuestRankGrantCache();
    }

    @Test
    void skipsReloadWhenPlayerAndCompletedHashMatch() {
        int hash = QuestRankGrantCache.hashCompleted(Set.of(EXPLORER, TITAN));
        assertFalse(cache.alreadyGranted(PLAYER_A, hash));
        cache.markGranted(PLAYER_A, hash);
        assertTrue(cache.alreadyGranted(PLAYER_A, hash));
        assertFalse(cache.alreadyGranted(PLAYER_B, hash));
        assertFalse(cache.alreadyGranted(PLAYER_A, hash + 1));
    }

    @Test
    void completedHashIsOrderIndependent() {
        Set<Long> left = new LinkedHashSet<>();
        left.add(TITAN);
        left.add(EXPLORER);
        Set<Long> right = new LinkedHashSet<>();
        right.add(EXPLORER);
        right.add(TITAN);
        assertEquals(QuestRankGrantCache.hashCompleted(left), QuestRankGrantCache.hashCompleted(right));
        assertEquals(0, QuestRankGrantCache.hashCompleted(Set.of()));
        assertEquals(0, QuestRankGrantCache.hashCompleted(null));
    }

    @Test
    void invalidatePlayerDoesNotClearTeammate() {
        int hash = QuestRankGrantCache.hashCompleted(Set.of(EXPLORER));
        cache.markGranted(PLAYER_A, hash);
        cache.markGranted(PLAYER_B, hash);
        cache.invalidatePlayer(PLAYER_A);
        assertFalse(cache.alreadyGranted(PLAYER_A, hash));
        assertTrue(cache.alreadyGranted(PLAYER_B, hash));
    }
}
