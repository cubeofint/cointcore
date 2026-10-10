package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketRestockTest {
    private static final MarketRestock.LotView DIAMOND = new MarketRestock.LotView(
            "lot-a",
            "owner-1",
            "minecraft:diamond",
            "",
            1L,
            100L,
            0L
    );
    private static final MarketRestock.LotView OLDER = new MarketRestock.LotView(
            "lot-old",
            "owner-1",
            "minecraft:diamond",
            "",
            2L,
            50L,
            0L
    );

    @Test
    void acceptCountStopsAtCapAndRejectsEmpty() {
        assertEquals(0, MarketRestock.acceptCount(10L, 10L, 64));
        assertEquals(0, MarketRestock.acceptCount(0L, 0L, 8));
        assertEquals(0, MarketRestock.acceptCount(5L, 20L, 0));
        assertEquals(10, MarketRestock.acceptCount(10L, 20L, 64));
        assertEquals(3, MarketRestock.acceptCount(0L, 100L, 3));
        assertEquals(1, MarketRestock.acceptCount(GlobalMarketMath.DEFAULT_MAX_STOCK_PER_LOT - 1L,
                GlobalMarketMath.DEFAULT_MAX_STOCK_PER_LOT, 8));
    }

    @Test
    void keysMatchIgnoresCountAndRequiresSameComponents() {
        assertTrue(MarketRestock.keysMatch("minecraft:diamond", "", "minecraft:diamond", ""));
        assertTrue(MarketRestock.keysMatch("minecraft:diamond", "{x}", "minecraft:diamond", "{x}"));
        assertFalse(MarketRestock.keysMatch("minecraft:diamond", "", "minecraft:emerald", ""));
        assertFalse(MarketRestock.keysMatch("minecraft:diamond", "{x}", "minecraft:diamond", ""));
        assertFalse(MarketRestock.keysMatch("", "", "minecraft:diamond", ""));
    }

    @Test
    void planPicksOldestMatchingLotAndRespectsCapBlacklistOwner() {
        MarketRestock.Decision hit = MarketRestock.plan(
                List.of(DIAMOND, OLDER),
                "owner-1",
                "minecraft:diamond",
                "",
                64,
                0L,
                GlobalMarketMath.DEFAULT_MAX_STOCK_PER_LOT,
                false
        );
        assertTrue(hit.ok());
        assertEquals("lot-old", hit.lotId());
        assertEquals(64, hit.accepted());

        assertFalse(MarketRestock.plan(List.of(DIAMOND), "owner-1", "minecraft:diamond", "", 8, 0L, 1_000_000L, true).ok());
        assertEquals("blacklist", MarketRestock.plan(List.of(DIAMOND), "owner-1", "minecraft:diamond", "", 8, 0L, 1_000_000L, true).reason());
        assertEquals("no_owner", MarketRestock.plan(List.of(DIAMOND), "", "minecraft:diamond", "", 8, 0L, 1_000_000L, false).reason());
        assertEquals("no_match", MarketRestock.plan(List.of(DIAMOND), "owner-1", "minecraft:emerald", "", 8, 0L, 1_000_000L, false).reason());
        assertEquals("cap", MarketRestock.plan(
                List.of(new MarketRestock.LotView("full", "owner-1", "minecraft:diamond", "", 10L, 1L, 0L)),
                "owner-1",
                "minecraft:diamond",
                "",
                8,
                0L,
                10L,
                false
        ).reason());
    }

    @Test
    void simulateDoesNotCommitAndCommitAddsStock() {
        MarketRestock.Decision plan = MarketRestock.plan(
                List.of(DIAMOND),
                "owner-1",
                "minecraft:diamond",
                "",
                7,
                0L,
                100L,
                false
        );
        assertEquals(7, plan.accepted());
        assertEquals(1L, MarketRestock.commitStock(DIAMOND.stockItems(), plan.accepted(), true));
        assertEquals(8L, MarketRestock.commitStock(DIAMOND.stockItems(), plan.accepted(), false));
        assertEquals(1L, MarketRestock.commitStock(DIAMOND.stockItems(), 0, false));
    }
}
