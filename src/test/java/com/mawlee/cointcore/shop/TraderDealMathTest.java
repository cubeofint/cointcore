package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraderDealMathTest {
    @Test
    void oneItemListingFillsAStack() {
        assertEquals(64, TraderDealMath.unitsPerStack(1, 64));
    }

    @Test
    void bulkListingUnitsAreStackDividedByCount() {
        assertEquals(4, TraderDealMath.unitsPerStack(16, 64));
        assertEquals(2, TraderDealMath.unitsPerStack(32, 64));
        assertEquals(1, TraderDealMath.unitsPerStack(64, 64));
    }

    @Test
    void countLargerThanStackStillAllowsOneUnit() {
        assertEquals(1, TraderDealMath.unitsPerStack(99, 64));
    }

    @Test
    void itemUnitsDivideOwnedByListingSize() {
        assertEquals(0, TraderDealMath.itemUnits(15, 16));
        assertEquals(2, TraderDealMath.itemUnits(32, 16));
        assertEquals(0, TraderDealMath.itemUnits(0, 1));
    }

    @Test
    void affordableUnitsRequireFullUnitCost() {
        assertEquals(0, TraderDealMath.affordableUnits(103L, 102L, 64));
        assertEquals(1, TraderDealMath.affordableUnits(103L, 103L, 64));
        assertEquals(3, TraderDealMath.affordableUnits(10L, 35L, 64));
        assertEquals(2, TraderDealMath.affordableUnits(10L, 1000L, 2));
    }

    @Test
    void resolveUnitsTakesTheTightestCap() {
        assertEquals(3, TraderDealMath.resolveUnits(64, 3, 10));
        assertEquals(2, TraderDealMath.resolveUnits(64, 10, 2));
        assertEquals(1, TraderDealMath.resolveUnits(1, 10, 10));
        assertEquals(0, TraderDealMath.resolveUnits(8, 0, 8));
    }

    @Test
    void costMultipliesAndSaturates() {
        assertEquals(309L, TraderDealMath.cost(103L, 3));
        assertEquals(0L, TraderDealMath.cost(103L, 0));
        assertEquals(Long.MAX_VALUE, TraderDealMath.cost(Long.MAX_VALUE / 2L + 1L, 3));
    }
}
