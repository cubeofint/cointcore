package com.mawlee.cointcore.luckperms;

import com.mawlee.cointcore.config.PrefixConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionMetaKeyTest {
    @Test
    void defaultSelectionKeyIsPrefixSelect() {
        assertEquals("cointcore-prefix-select", PrefixConfig.DEFAULT_META_KEY);
        assertEquals("cointcore-prefix-select", LuckPermsUsers.PREFIX_SELECT_KEY);
        assertTrue(LuckPermsUsers.isSelectionMetaKey("cointcore-prefix-select", null));
        assertTrue(LuckPermsUsers.isSelectionMetaKey("cointcore-quest-prefix", null));
        assertFalse(LuckPermsUsers.isSelectionMetaKey("unrelated", null));
    }
}
