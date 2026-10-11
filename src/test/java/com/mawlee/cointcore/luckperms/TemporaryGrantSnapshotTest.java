package com.mawlee.cointcore.luckperms;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporaryGrantSnapshotTest {
    @Test
    void earliestActiveUsesOwnAndGroupGrantsWithoutResolvedCopy() {
        Instant first = Instant.parse("2026-10-11T00:00:10Z");
        Instant second = Instant.parse("2026-10-11T00:00:20Z");
        TemporaryGrantSnapshot snapshot = TemporaryGrantSnapshot.of(List.of(
                new TemporaryGrantSnapshot.Grant("cointcore.fly", false, false, second),
                new TemporaryGrantSnapshot.Grant("cointcore.god", false, false, first),
                new TemporaryGrantSnapshot.Grant("cointcore.fly", false, true, Instant.parse("2026-10-11T00:00:01Z"))
        ));
        assertEquals(second, snapshot.earliestActive("cointcore.fly").orElseThrow());
        assertEquals(first, snapshot.earliestActive("cointcore.god").orElseThrow());
        assertTrue(snapshot.earliestActive("cointcore.other").isEmpty());
    }

    @Test
    void inheritanceGrantMatchesAnyPermission() {
        Instant expiry = Instant.parse("2026-10-11T00:01:00Z");
        TemporaryGrantSnapshot snapshot = TemporaryGrantSnapshot.of(List.of(
                new TemporaryGrantSnapshot.Grant(null, true, false, expiry)
        ));
        assertEquals(expiry, snapshot.earliestActive("cointcore.fly").orElseThrow());
        assertFalse(snapshot.hadExpired("cointcore.fly"));
    }

    @Test
    void expiredInheritanceIsVisible() {
        TemporaryGrantSnapshot snapshot = TemporaryGrantSnapshot.of(List.of(
                new TemporaryGrantSnapshot.Grant(null, true, true, Instant.parse("2026-10-11T00:00:00Z"))
        ));
        assertTrue(snapshot.hadExpired("cointcore.god"));
        assertFalse(snapshot.earliestActive("cointcore.god").isPresent());
    }
}
