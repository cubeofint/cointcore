package com.mawlee.cointcore.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PagerChromeTest {
    @Test
    void hidesEverythingOnASinglePage() {
        assertFalse(PagerChrome.showLabel(0));
        assertFalse(PagerChrome.showPrev(0, 0));
        assertFalse(PagerChrome.showNext(0, 0));
    }

    @Test
    void hidesFirstAndLastArrows() {
        assertFalse(PagerChrome.showPrev(0, 3));
        assertTrue(PagerChrome.showNext(0, 3));
        assertTrue(PagerChrome.showPrev(3, 3));
        assertFalse(PagerChrome.showNext(3, 3));
        assertTrue(PagerChrome.showLabel(3));
    }
}
