package com.mawlee.cointcore.luckperms;

import com.mawlee.cointcore.prefix.PrefixSelectionMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionMetaKeyTest {
    @Test
    void defaultSelectionKeyIsPrefixSelect() {
        assertEquals("cointcore-prefix-select", PrefixSelectionMeta.KEY);
        assertEquals("cointcore-quest-prefix", PrefixSelectionMeta.LEGACY_KEY);
        assertTrue(PrefixSelectionMeta.isSelectionKey("cointcore-prefix-select", null));
        assertTrue(PrefixSelectionMeta.isSelectionKey("cointcore-quest-prefix", null));
        assertTrue(PrefixSelectionMeta.isSelectionKey("custom-key", "custom-key"));
        assertTrue(PrefixSelectionMeta.isCanonicalKey("cointcore-prefix-select"));
        assertFalse(PrefixSelectionMeta.isCanonicalKey("cointcore-quest-prefix"));
        assertFalse(PrefixSelectionMeta.isSelectionKey("unrelated", null));
    }
}
