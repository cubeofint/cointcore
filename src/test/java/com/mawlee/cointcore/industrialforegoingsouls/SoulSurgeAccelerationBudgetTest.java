package com.mawlee.cointcore.industrialforegoingsouls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoulSurgeAccelerationBudgetTest {
    private static final long TARGET = SoulSurgeAccelerationBudget.pack(4, 64, 7);
    private static final int ACCELERATION_TICK = 4;

    private SoulSurgeAccelerationBudget budget;

    @BeforeEach
    void newBudget() {
        budget = new SoulSurgeAccelerationBudget();
    }

    @Test
    void firstThreeExtraTicksApplyFourthIsRejected() {
        assertEquals(3, SoulSurgeAcceleration.MAX_EXTRA_TICKS);
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 10L));
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 10L));
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 10L));
        assertFalse(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 10L));
        assertEquals(3, budget.extraTicksApplied(TARGET, 10L));
        assertFalse(budget.hasRemaining(TARGET, 10L));
    }

    @Test
    void oneSurgeAccelerationTickLoopCapsAtThreeExtra() {
        int applied = 0;
        for (int i = 0; i < ACCELERATION_TICK; i++) {
            if (SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 1L)) {
                applied++;
            }
        }
        assertEquals(3, applied);
        assertEquals(3, budget.extraTicksApplied(TARGET, 1L));
    }

    @Test
    void sixSurgesShareThreeExtraTicks() {
        int applied = 0;
        for (int i = 0; i < 6; i++) {
            if (SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 2L)) {
                applied++;
            }
        }
        assertEquals(3, applied);
        assertFalse(budget.hasRemaining(TARGET, 2L));
    }

    @Test
    void twoSurgesSplitRemainingBudget() {
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 3L));
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 3L));
        assertEquals(2, budget.extraTicksApplied(TARGET, 3L));
        assertTrue(budget.hasRemaining(TARGET, 3L));

        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 3L));
        assertFalse(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 3L));
        assertEquals(3, budget.extraTicksApplied(TARGET, 3L));
    }

    @Test
    void ringOfSixSurgesSharesTheSameTargetBudget() {
        int[] machine = {4, 64, 7};
        record Side(String facing, int x, int y, int z) {
        }
        Side[] ring = {
                new Side("up", 4, 65, 7),
                new Side("down", 4, 63, 7),
                new Side("north", 4, 64, 6),
                new Side("south", 4, 64, 8),
                new Side("west", 3, 64, 7),
                new Side("east", 5, 64, 7)
        };

        int applied = 0;
        for (Side side : ring) {
            int[] target = SoulSurgeTarget.attachedCoordinates(side.x, side.y, side.z, side.facing);
            assertEquals(machine[0], target[0], side.facing);
            assertEquals(machine[1], target[1], side.facing);
            assertEquals(machine[2], target[2], side.facing);
            assertTrue(SoulSurgeTarget.shouldAccelerate(false, true, "minecraft:furnace"));
            assertFalse(SoulSurgeTarget.shouldAccelerate(
                    false, true, SoulSurgeTarget.SOUL_SURGE_BLOCK_ID
            ));

            long targetPacked = SoulSurgeAccelerationBudget.pack(target[0], target[1], target[2]);
            if (SoulSurgeAcceleration.tryApplyExtraTick(budget, targetPacked, 1L)) {
                applied++;
            }
        }
        assertEquals(3, applied);
        assertEquals(3, budget.extraTicksApplied(TARGET, 1L));
    }

    @Test
    void packRoundTripIncludingNegatives() {
        assertPacked(-8, -64, 12);
        assertPacked(0, 0, 0);
        assertPacked(30000000, 319, -30000000);
    }

    @Test
    void nextGameTickResetsBudget() {
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 8L));
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 8L));
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 8L));
        assertFalse(budget.hasRemaining(TARGET, 8L));

        assertEquals(0, budget.extraTicksApplied(TARGET, 9L));
        assertTrue(budget.hasRemaining(TARGET, 9L));
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 9L));
        assertEquals(1, budget.extraTicksApplied(TARGET, 9L));
    }

    @Test
    void skippedGameTimeClearsStaleEntries() {
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 4L));
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 4L));
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 4L));

        assertEquals(0, budget.extraTicksApplied(TARGET, 20L));
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 20L));
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 20L));
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 20L));
        assertFalse(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 20L));
    }

    @Test
    void differentTargetsAreIndependent() {
        long other = SoulSurgeAccelerationBudget.pack(0, 0, 0);
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 1L));
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 1L));
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 1L));
        assertFalse(SoulSurgeAcceleration.tryApplyExtraTick(budget, TARGET, 1L));

        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, other, 1L));
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, other, 1L));
        assertTrue(SoulSurgeAcceleration.tryApplyExtraTick(budget, other, 1L));
        assertFalse(SoulSurgeAcceleration.tryApplyExtraTick(budget, other, 1L));
        assertEquals(3, budget.extraTicksApplied(TARGET, 1L));
        assertEquals(3, budget.extraTicksApplied(other, 1L));
    }

    @Test
    void unusedTargetReportsZeroExtraTicks() {
        assertEquals(0, budget.extraTicksApplied(TARGET, 1L));
        assertTrue(budget.hasRemaining(TARGET, 1L));
    }

    private static void assertPacked(int x, int y, int z) {
        long packed = SoulSurgeAccelerationBudget.pack(x, y, z);
        assertEquals(x, SoulSurgeAccelerationBudget.unpackX(packed));
        assertEquals(y, SoulSurgeAccelerationBudget.unpackY(packed));
        assertEquals(z, SoulSurgeAccelerationBudget.unpackZ(packed));
    }
}
