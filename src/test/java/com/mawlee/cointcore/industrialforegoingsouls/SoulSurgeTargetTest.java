package com.mawlee.cointcore.industrialforegoingsouls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoulSurgeTargetTest {
    @ParameterizedTest
    @CsvSource({
            "up,    4, 10, 7, 4, 9, 7",
            "down,  4, 10, 7, 4, 11, 7",
            "north, 4, 10, 7, 4, 10, 8",
            "south, 4, 10, 7, 4, 10, 6",
            "west,  4, 10, 7, 5, 10, 7",
            "east,  4, 10, 7, 3, 10, 7",
            "UP,    0, 0, 0, 0, -1, 0",
            "North, 1, 2, 3, 1, 2, 4"
    })
    void attachedCellIsFacingOpposite(
            String facing,
            int x,
            int y,
            int z,
            int expectedX,
            int expectedY,
            int expectedZ
    ) {
        assertArrayEquals(
                new int[]{expectedX, expectedY, expectedZ},
                SoulSurgeTarget.attachedCoordinates(x, y, z, facing)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "sideways", "facing", "unknown"})
    void missingFacingTargetsBlockBelow(String facing) {
        assertEquals("up", SoulSurgeTarget.normalizeFacing(facing));
        assertArrayEquals(new int[]{8, 63, -2}, SoulSurgeTarget.attachedCoordinates(8, 64, -2, facing));
    }

    @Test
    void soulSurgeIdIsExact() {
        assertTrue(SoulSurgeTarget.isSoulSurgeId("industrialforegoingsouls:soul_surge"));
        assertFalse(SoulSurgeTarget.isSoulSurgeId("industrialforegoing:ore_laser_base"));
        assertFalse(SoulSurgeTarget.isSoulSurgeId("minecraft:furnace"));
        assertFalse(SoulSurgeTarget.isSoulSurgeId(null));
        assertFalse(SoulSurgeTarget.isSoulSurgeId(""));
    }

    @Test
    void onlyValidBlockEntitiesAccelerate() {
        assertTrue(SoulSurgeTarget.shouldAccelerate(false, true, "minecraft:furnace"));
        assertTrue(SoulSurgeTarget.shouldAccelerate(false, true, "mekanism:enrichment_chamber"));
        assertFalse(SoulSurgeTarget.shouldAccelerate(true, false, "minecraft:air"));
        assertFalse(SoulSurgeTarget.shouldAccelerate(false, false, "minecraft:dirt"));
        assertFalse(SoulSurgeTarget.shouldAccelerate(
                false, true, SoulSurgeTarget.SOUL_SURGE_BLOCK_ID
        ));
    }
}
