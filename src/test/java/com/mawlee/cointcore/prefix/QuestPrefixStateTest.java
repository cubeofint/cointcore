package com.mawlee.cointcore.prefix;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestPrefixStateTest {
    @Test
    void noWriteWhenOwnPrefixAndMetaAlreadyMatch() {
        QuestPrefixState current = QuestPrefixState.current("titan", "&6[Titan] ", 1, 1);
        QuestPrefixState desired = QuestPrefixState.desired("Titan", "&6[Titan] ");
        assertFalse(current.needsWrite(desired));
    }

    @Test
    void noWriteWhenAutoRenderAlreadyApplied() {
        QuestPrefixState current = QuestPrefixState.current(null, "&6[Titan] ", 1, 0);
        QuestPrefixState desired = QuestPrefixState.desired(null, "&6[Titan] ");
        assertFalse(current.needsWrite(desired));
    }

    @Test
    void noWriteWhenClearedStateAlreadyEmpty() {
        QuestPrefixState current = QuestPrefixState.current(null, null, 0, 0);
        QuestPrefixState desired = QuestPrefixState.desired(null, "  ");
        assertFalse(current.needsWrite(desired));
    }

    @Test
    void writeWhenPrefixTextDiffers() {
        QuestPrefixState current = QuestPrefixState.current(null, "&6[Titan] ", 1, 0);
        QuestPrefixState desired = QuestPrefixState.desired(null, "&b[Jesus] ");
        assertTrue(current.needsWrite(desired));
    }

    @Test
    void writeWhenDuplicateOwnPrefixNodesRemain() {
        QuestPrefixState current = QuestPrefixState.current("titan", "&6[Titan] ", 2, 1);
        QuestPrefixState desired = QuestPrefixState.desired("titan", "&6[Titan] ");
        assertTrue(current.needsWrite(desired));
    }

    @Test
    void writeWhenSelectionMetaIsOnLegacyKey() {
        QuestPrefixState current = QuestPrefixState.current("titan", "&6[Titan] ", 1, 1, true);
        QuestPrefixState desired = QuestPrefixState.desired("titan", "&6[Titan] ");
        assertTrue(current.needsWrite(desired));
    }

    @Test
    void writeWhenLegacyPriority1000NodeRemains() {
        QuestPrefixState current = QuestPrefixState.current("titan", "&6[Titan] ", 1, 1, false, true);
        QuestPrefixState desired = QuestPrefixState.desired("titan", "&6[Titan] ");
        assertTrue(current.needsWrite(desired));
    }
}
