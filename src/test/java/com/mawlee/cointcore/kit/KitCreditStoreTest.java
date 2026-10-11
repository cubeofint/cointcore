package com.mawlee.cointcore.kit;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitCreditStoreTest {
    private static final UUID PLAYER = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
    private static final UUID OTHER = UUID.fromString("f3fc162d-d344-32fa-8c9a-0b987b0791cf");

    @Test
    void creditsOfflinePlayerByUuidWithoutName() {
        KitCreditStore store = new KitCreditStore();
        assertEquals(3, store.add(PLAYER, "vip", 3));
        assertEquals(3, store.get(PLAYER, "vip"));
        assertEquals(Map.of("vip", 3), store.all(PLAYER));
        assertTrue(store.all(OTHER).isEmpty());
    }

    @Test
    void remainingAfterSpend() {
        KitCreditStore store = new KitCreditStore();
        store.add(PLAYER, "food", 2);
        assertEquals(1, store.take(PLAYER, "food", 1));
        assertEquals(1, store.get(PLAYER, "food"));
        assertEquals(0, store.take(PLAYER, "food", 1));
        assertTrue(store.all(PLAYER).isEmpty());
    }

    @Test
    void takeDoesNotGoNegative() {
        KitCreditStore store = new KitCreditStore();
        store.add(PLAYER, "ore", 1);
        assertEquals(0, store.take(PLAYER, "ore", 5));
        assertEquals(0, store.get(PLAYER, "ore"));
    }

    @Test
    void kitNameIsCaseInsensitive() {
        KitCreditStore store = new KitCreditStore();
        store.add(PLAYER, "VIP", 1);
        assertEquals(2, store.add(PLAYER, "vip", 1));
        assertEquals(2, store.get(PLAYER, "Vip"));
        assertEquals(Map.of("vip", 2), store.all(PLAYER));
    }

    @Test
    void clearPlayerAndSingleKit() {
        KitCreditStore store = new KitCreditStore();
        store.add(PLAYER, "a", 2);
        store.add(PLAYER, "b", 4);
        assertEquals(2, store.clearKit(PLAYER, "a"));
        assertEquals(Map.of("b", 4), store.all(PLAYER));
        assertEquals(Map.of("b", 4), store.clearAll(PLAYER));
        assertTrue(store.all(PLAYER).isEmpty());
        assertTrue(store.isEmpty());
    }

    @Test
    void replaceAllLoadsPlainCounts() {
        KitCreditStore store = new KitCreditStore();
        store.replaceAll(Map.of(
                PLAYER, Map.of("starter", 5, "empty", 0),
                OTHER, Map.of("vip", 1)
        ));
        assertEquals(5, store.get(PLAYER, "starter"));
        assertEquals(0, store.get(PLAYER, "empty"));
        assertEquals(1, store.get(OTHER, "VIP"));
        assertEquals(Map.of(PLAYER, Map.of("starter", 5), OTHER, Map.of("vip", 1)), store.snapshot());
    }

    @Test
    void addSaturatesAtIntegerMax() {
        KitCreditStore store = new KitCreditStore();
        store.set(PLAYER, "cap", Integer.MAX_VALUE - 1);
        assertEquals(Integer.MAX_VALUE, store.add(PLAYER, "cap", 5));
    }
}
