package com.mawlee.cointcore.invsee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvSeeTabLayoutTest {
    @Test
    void wrapsIntoTwoRowsWhenThatFits() {
        int[] widths = {70, 90, 70, 24, 42, 70};
        InvSeeTabLayout.Result result = InvSeeTabLayout.compute(widths, 256, 0, 14, 2);
        assertFalse(result.scrolled());
        assertEquals(2, result.rows());
        assertEquals(6, result.placements().size());
        assertTrue(result.placements().stream().allMatch(p -> p.row() <= 1));
        assertNoOverlap(result);
    }

    @Test
    void scrollsWhenTwoRowsCannotFit() {
        int[] widths = {80, 90, 80, 80, 80, 80};
        InvSeeTabLayout.Result result = InvSeeTabLayout.compute(widths, 176, 0, 14, 2);
        assertTrue(result.scrolled());
        assertEquals(1, result.rows());
        assertTrue(result.canScrollRight());
        assertFalse(result.canScrollLeft());
        assertNoOverlap(result);
    }

    @Test
    void scrollAdvancesWindow() {
        int[] widths = {80, 90, 80, 80, 80, 80};
        InvSeeTabLayout.Result first = InvSeeTabLayout.compute(widths, 176, 0, 14, 2);
        InvSeeTabLayout.Result next = InvSeeTabLayout.compute(widths, 176, 2, 14, 2);
        assertTrue(next.placements().getFirst().tabIndex() >= first.placements().getFirst().tabIndex());
        assertTrue(next.canScrollLeft());
        assertNoOverlap(next);
    }

    @Test
    void tabWidthUsesTextPlusPadding() {
        assertEquals(40, InvSeeTabLayout.tabWidth(28, 12, 24));
        assertEquals(24, InvSeeTabLayout.tabWidth(4, 8, 24));
    }

    private static void assertNoOverlap(InvSeeTabLayout.Result result) {
        var placements = result.placements();
        for (int i = 0; i < placements.size(); i++) {
            for (int j = i + 1; j < placements.size(); j++) {
                var a = placements.get(i);
                var b = placements.get(j);
                if (a.row() != b.row()) {
                    continue;
                }
                int aEnd = a.x() + a.width();
                int bEnd = b.x() + b.width();
                boolean overlap = a.x() < bEnd && b.x() < aEnd;
                assertFalse(overlap, a + " overlaps " + b);
            }
        }
    }
}
