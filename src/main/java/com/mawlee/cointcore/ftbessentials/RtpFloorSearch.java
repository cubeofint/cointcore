package com.mawlee.cointcore.ftbessentials;

import java.util.function.IntPredicate;

/**
 * Column search for FTB Essentials {@code /rtp} in dimensions with a bedrock roof: the heightmap there points
 * at the roof, so {@code /rtp} lands players on (or in) bedrock. This walks down through the roof into the
 * first open space and returns the lowest-risk standing spot: a safe floor block with two free blocks above.
 */
public final class RtpFloorSearch {
    public static final int NOT_FOUND = Integer.MIN_VALUE;

    private RtpFloorSearch() {
    }

    /**
     * @param roofTopY first free Y above the roof (the heightmap result)
     * @param minY     lowest Y to consider as a floor
     * @param isFree   block at Y is passable for a player (air, no fluid)
     * @param isFloor  block at Y is a safe floor (solid, not bedrock, not a fluid)
     * @return the first free Y above the found floor (heightmap semantics), or {@link #NOT_FOUND}
     */
    public static int findStandY(int roofTopY, int minY, IntPredicate isFree, IntPredicate isFloor) {
        int y = roofTopY - 1;
        // Pass through the roof into the first open space below it.
        while (y > minY && !isFree.test(y)) {
            y--;
        }
        for (; y > minY; y--) {
            if (isFloor.test(y) && isFree.test(y + 1) && isFree.test(y + 2)) {
                return y + 1;
            }
        }
        return NOT_FOUND;
    }
}
