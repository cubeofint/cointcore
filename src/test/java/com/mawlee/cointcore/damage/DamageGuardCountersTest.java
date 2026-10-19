package com.mawlee.cointcore.damage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageGuardCountersTest {
    @Test
    void tracksDepthAndResetsHitsOnlyForTouchedTargets() {
        DamageGuardCounters counters = new DamageGuardCounters();
        assertEquals(1, counters.enter());
        assertEquals(2, counters.enter());
        assertEquals(2, counters.currentDepth());
        counters.exit();
        assertEquals(1, counters.currentDepth());
        counters.exit();
        assertEquals(0, counters.currentDepth());

        assertEquals(1, counters.recordHit(10));
        assertEquals(2, counters.recordHit(10));
        assertEquals(1, counters.recordHit(11));
        assertEquals(2, counters.touchedCount());
        assertTrue(counters.shouldBlock(10, 1, 8, 1));
        assertFalse(counters.shouldBlock(11, 1, 8, 20));

        counters.markBlocked(11);
        assertTrue(counters.isBlocked(11));
        counters.resetTick();
        assertEquals(0, counters.hits(10));
        assertEquals(0, counters.touchedCount());
        assertFalse(counters.isBlocked(11));
        assertFalse(counters.shouldBlock(10, 1, 8, 20));
    }

    @Test
    void stackSummaryKeepsDistinctModPackages() {
        StackTraceElement[] stack = new StackTraceElement[] {
                new StackTraceElement("java.lang.Thread", "run", "Thread.java", 1),
                new StackTraceElement("io.redspace.ironsspellbooks.spells.EchoingHits", "onHit", "EchoingHits.java", 10),
                new StackTraceElement("dev.shadowsoffire.apotheosis.affix.Execute", "hurt", "Execute.java", 20),
                new StackTraceElement("io.redspace.ironsspellbooks.spells.EchoingHits", "bounce", "EchoingHits.java", 11),
                new StackTraceElement("net.minecraft.world.entity.LivingEntity", "hurt", "LivingEntity.java", 30)
        };
        String summary = DamageStackSummary.of(stack);
        assertTrue(summary.contains("io.redspace.ironsspellbooks.spells"));
        assertTrue(summary.contains("dev.shadowsoffire.apotheosis.affix"));
        assertTrue(summary.contains("net.minecraft.world.entity"));
        assertFalse(summary.contains("java.lang"));
    }
}
