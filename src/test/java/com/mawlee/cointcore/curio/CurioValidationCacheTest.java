package com.mawlee.cointcore.curio;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CurioValidationCacheTest {
    @Test void onlyMetadataChangesOnAnAlreadyValidatedPlayerCanSkipTheScan() {
        var cache = new CurioValidationCache<Object>();
        var player = new Object();
        assertTrue(cache.needsValidation(player, 1, true, 1, 1));
        cache.validated(player, 1);
        assertFalse(cache.needsValidation(player, 1, true, 1, 1));
        assertTrue(cache.needsValidation(player, 1, false, 1, 1));
        assertTrue(cache.needsValidation(player, 1, true, 1, 2));
        assertTrue(cache.needsValidation(player, 1, true, 0, 1));
        assertTrue(cache.needsValidation(new Object(), 1, true, 1, 1));
    }

    @Test void reloadingLimitsForcesValidationAgain() {
        var cache = new CurioValidationCache<Object>();
        var player = new Object();
        cache.validated(player, 1);
        assertTrue(cache.needsValidation(player, 2, true, 1, 1));
        cache.validated(player, 2);
        assertFalse(cache.needsValidation(player, 2, true, 1, 1));
    }
}
