package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteMarketFlagTest {
    @Test
    void remoteMarketDefaultsOnWhenUnset() {
        assertTrue(RemoteMarketFlag.enabled(null));
        assertTrue(RemoteMarketFlag.enabled(true));
    }

    @Test
    void remoteMarketCanBeDisabled() {
        assertFalse(RemoteMarketFlag.enabled(false));
    }
}
