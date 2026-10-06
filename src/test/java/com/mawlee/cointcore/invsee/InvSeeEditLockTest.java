package com.mawlee.cointcore.invsee;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvSeeEditLockTest {
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void firstViewerGetsTheLock() {
        InvSeeEditLock lock = new InvSeeEditLock(1_000L);
        assertTrue(lock.tryAcquire(ALICE, "Alice", 0L));
        assertTrue(lock.isHeldBy(ALICE));
        assertEquals("Alice", lock.editorName());
    }

    @Test
    void secondViewerIsRejectedWhileLockIsHeld() {
        InvSeeEditLock lock = new InvSeeEditLock(1_000L);
        assertTrue(lock.tryAcquire(ALICE, "Alice", 0L));
        assertFalse(lock.tryAcquire(BOB, "Bob", 100L));
        assertTrue(lock.isHeldBy(ALICE));
    }

    @Test
    void sameViewerCanReacquire() {
        InvSeeEditLock lock = new InvSeeEditLock(1_000L);
        assertTrue(lock.tryAcquire(ALICE, "Alice", 0L));
        assertTrue(lock.tryAcquire(ALICE, "Alice", 50L));
    }

    @Test
    void idleLockExpiresAndCanBeTaken() {
        InvSeeEditLock lock = new InvSeeEditLock(1_000L);
        assertTrue(lock.tryAcquire(ALICE, "Alice", 0L));
        assertTrue(lock.expireIfIdle(1_000L));
        assertFalse(lock.isHeld());
        assertTrue(lock.tryAcquire(BOB, "Bob", 1_000L));
        assertTrue(lock.isHeldBy(BOB));
    }

    @Test
    void touchExtendsIdleTimeout() {
        InvSeeEditLock lock = new InvSeeEditLock(1_000L);
        assertTrue(lock.tryAcquire(ALICE, "Alice", 0L));
        lock.touch(ALICE, 900L);
        assertFalse(lock.expireIfIdle(1_500L));
        assertTrue(lock.isHeldBy(ALICE));
        assertTrue(lock.expireIfIdle(1_900L));
    }

    @Test
    void onlyHolderCanRelease() {
        InvSeeEditLock lock = new InvSeeEditLock(1_000L);
        assertTrue(lock.tryAcquire(ALICE, "Alice", 0L));
        assertFalse(lock.release(BOB));
        assertTrue(lock.isHeldBy(ALICE));
        assertTrue(lock.release(ALICE));
        assertFalse(lock.isHeld());
    }

    @Test
    void forceReleaseClearsHolder() {
        InvSeeEditLock lock = new InvSeeEditLock(1_000L);
        assertTrue(lock.tryAcquire(ALICE, "Alice", 0L));
        lock.forceRelease();
        assertFalse(lock.isHeld());
        assertTrue(lock.tryAcquire(BOB, "Bob", 10L));
    }
}
