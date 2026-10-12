package com.mawlee.cointcore.questrank;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestRankOnceTest {
    @AfterEach
    void tearDown() {
        QuestRankOnce.reset();
    }

    @Test
    void firstCallIsTrueThenFalseForSameKey() {
        assertTrue(QuestRankOnce.first("player:abc"));
        assertFalse(QuestRankOnce.first("player:abc"));
        assertTrue(QuestRankOnce.already("player:abc"));
        assertTrue(QuestRankOnce.first("player:def"));
    }

    @Test
    void guardReturnsFallbackAndDoesNotRethrow() {
        Integer value = QuestRankOnce.guard("player:throw", 7, () -> {
            throw new IllegalArgumentException("argument type mismatch");
        });
        assertEquals(7, value);
        assertTrue(QuestRankOnce.already("player:throw"));
        assertEquals(8, QuestRankOnce.guard("player:ok", 0, () -> 8));
    }
}
