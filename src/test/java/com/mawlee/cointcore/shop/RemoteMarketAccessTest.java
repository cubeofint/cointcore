package com.mawlee.cointcore.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteMarketAccessTest {
    @Test
    void bothConfigAndPermissionAreRequired() {
        assertTrue(RemoteMarketAccess.allowed(true, true));
        assertFalse(RemoteMarketAccess.allowed(false, true));
        assertFalse(RemoteMarketAccess.allowed(true, false));
        assertFalse(RemoteMarketAccess.allowed(false, false));
    }
}
