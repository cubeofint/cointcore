package com.mawlee.cointcore.crowded;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrowdedMobPolicyTest {
    @Test
    void playersNeverEligible() {
        assertFalse(CrowdedMobPolicy.isEligible(
                true, false, false, false, false, true, true, true, true
        ));
    }

    @Test
    void bossesAndVillagersExcludedUnlessOptedIn() {
        assertFalse(CrowdedMobPolicy.isEligible(
                false, true, false, false, false, false, false, false, false
        ));
        assertTrue(CrowdedMobPolicy.isEligible(
                false, true, false, false, false, true, false, false, false
        ));
        assertFalse(CrowdedMobPolicy.isEligible(
                false, false, true, false, false, false, false, false, false
        ));
        assertTrue(CrowdedMobPolicy.isEligible(
                false, false, true, false, false, false, true, false, false
        ));
    }

    @Test
    void namedAndLeashedExcludedUnlessOptedIn() {
        assertFalse(CrowdedMobPolicy.isEligible(
                false, false, false, true, false, false, false, false, false
        ));
        assertTrue(CrowdedMobPolicy.isEligible(
                false, false, false, true, false, false, false, true, false
        ));
        assertFalse(CrowdedMobPolicy.isEligible(
                false, false, false, false, true, false, false, false, false
        ));
        assertTrue(CrowdedMobPolicy.isEligible(
                false, false, false, false, true, false, false, false, true
        ));
    }

    @Test
    void parseEntityIdsDropsInvalid() {
        Set<String> ids = CrowdedMobPolicy.parseEntityIds(
                List.of("minecraft:chicken", "not a valid id!!!", "minecraft:cow"),
                ignored -> {
                }
        );
        assertEquals(Set.of("minecraft:chicken", "minecraft:cow"), ids);
        assertEquals(8, CrowdedMobPolicy.clamp(8, 2, 256));
        assertEquals(2, CrowdedMobPolicy.clamp(1, 2, 256));
        assertEquals(1.0D, CrowdedMobPolicy.clamp(1.0D, 0.25D, 8.0D));
    }

    @Test
    void denyListWinsAndAllowListRestricts() {
        assertFalse(CrowdedMobPolicy.isTypeAllowed(
                "minecraft:chicken", Set.of(), Set.of("minecraft:chicken")
        ));
        assertTrue(CrowdedMobPolicy.isTypeAllowed(
                "minecraft:chicken", Set.of(), Set.of("minecraft:cow")
        ));
        assertFalse(CrowdedMobPolicy.isTypeAllowed(
                "minecraft:chicken", Set.of("minecraft:cow"), Set.of()
        ));
        assertTrue(CrowdedMobPolicy.isTypeAllowed(
                "minecraft:chicken", Set.of("minecraft:chicken"), Set.of()
        ));
    }

    @Test
    void crowdedAtThresholdAndAiThrottle() {
        assertFalse(CrowdedMobPolicy.isCrowdedCount(7, 8));
        assertTrue(CrowdedMobPolicy.isCrowdedCount(8, 8));
        assertTrue(CrowdedMobPolicy.shouldRecheck(30, 30));
        assertFalse(CrowdedMobPolicy.shouldRecheck(29, 30));
        assertEquals(45, CrowdedMobPolicy.nextCheckTick(15, 30));
        assertFalse(CrowdedMobPolicy.shouldSkipAi(false, true, true, 1, 0, 15));
        assertFalse(CrowdedMobPolicy.shouldSkipAi(true, false, true, 1, 0, 15));
        assertTrue(CrowdedMobPolicy.shouldSkipAi(true, true, true, 1, 0, 15));
        assertFalse(CrowdedMobPolicy.shouldSkipAi(true, true, true, 15, 0, 15));
        assertTrue(CrowdedMobPolicy.shouldDisablePush(true, true, true));
        assertFalse(CrowdedMobPolicy.shouldDisablePush(false, true, true));
        assertTrue(CrowdedMobPolicy.shouldSkipAmbient(true, true, true));
        assertFalse(CrowdedMobPolicy.shouldSkipAmbient(true, true, false));
    }
}
