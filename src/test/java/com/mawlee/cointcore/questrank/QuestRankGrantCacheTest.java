package com.mawlee.cointcore.questrank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRankGrantCacheTest {
    private static final UUID PLAYER_A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID PLAYER_B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID TEAM = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID OTHER_TEAM = UUID.fromString("00000000-0000-0000-0000-0000000000c2");
    private static final long EXPLORER = QuestRankIds.parseHex("3FABD06F3F6D3BC3").orElseThrow();
    private static final long TITAN = QuestRankIds.parseHex("6491B39FBC0C53B2").orElseThrow();

    private QuestRankGrantCache cache;

    @BeforeEach
    void setUp() {
        cache = new QuestRankGrantCache();
    }

    @Test
    void skipsReloadWhenPlayerTeamAndCompletedHashMatch() {
        int hash = QuestRankGrantCache.hashCompleted(Set.of(EXPLORER, TITAN));
        assertFalse(cache.alreadyGranted(PLAYER_A, TEAM, hash));
        cache.markGranted(PLAYER_A, TEAM, hash);
        assertTrue(cache.alreadyGranted(PLAYER_A, TEAM, hash));
        assertFalse(cache.alreadyGranted(PLAYER_B, TEAM, hash));
        assertFalse(cache.alreadyGranted(PLAYER_A, OTHER_TEAM, hash));
        assertFalse(cache.alreadyGranted(PLAYER_A, TEAM, hash + 1));
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
    void invalidatesWhenCompletedSetOrMemberListChanges() {
        int first = QuestRankGrantCache.hashCompleted(Set.of(EXPLORER));
        int second = QuestRankGrantCache.hashCompleted(Set.of(EXPLORER, TITAN));
        cache.updateTeamState(TEAM, Set.of(PLAYER_A), first);
        cache.markGranted(PLAYER_A, TEAM, first);
        assertTrue(cache.alreadyGranted(PLAYER_A, TEAM, first));

        assertTrue(cache.updateTeamState(TEAM, Set.of(PLAYER_A), second));
        assertFalse(cache.alreadyGranted(PLAYER_A, TEAM, first));
        assertFalse(cache.alreadyGranted(PLAYER_A, TEAM, second));

        cache.markGranted(PLAYER_A, TEAM, second);
        assertTrue(cache.updateTeamState(TEAM, Set.of(PLAYER_A, PLAYER_B), second));
        assertFalse(cache.alreadyGranted(PLAYER_A, TEAM, second));
        assertFalse(cache.updateTeamState(TEAM, Set.of(PLAYER_A, PLAYER_B), second));
    }

    @Test
    void firstTeamSightingIsNotAJoin() {
        assertTrue(cache.trackTeam(PLAYER_A, TEAM).isEmpty());
        assertTrue(cache.trackTeam(PLAYER_A, TEAM).isEmpty());
    }

    @Test
    void teamIdChangeIsDetectedImmediately() {
        cache.trackTeam(PLAYER_A, TEAM);
        Optional<UUID> previous = cache.trackTeam(PLAYER_A, OTHER_TEAM);
        assertTrue(previous.isPresent());
        assertEquals(TEAM, previous.orElseThrow());
    }
}
