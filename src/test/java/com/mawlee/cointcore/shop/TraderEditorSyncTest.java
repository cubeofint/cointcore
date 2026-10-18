package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraderEditorSyncTest {
    @Test
    void backgroundRefreshDoesNotTouchEditorOrStatus() {
        assertFalse(TraderEditorSync.showActionStatus(false));
        assertFalse(TraderEditorSync.applyServerValuesToEditor(false, true, false));
        assertFalse(TraderEditorSync.applyServerValuesToEditor(false, true, true));
    }

    @Test
    void successfulUserActionAppliesOnlyWhenNotTyping() {
        assertTrue(TraderEditorSync.showActionStatus(true));
        assertTrue(TraderEditorSync.applyServerValuesToEditor(true, true, false));
        assertFalse(TraderEditorSync.applyServerValuesToEditor(true, true, true));
        assertFalse(TraderEditorSync.applyServerValuesToEditor(true, false, false));
    }
}
