package com.mawlee.cointcore.mekmm;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MekmmWindGeneratorNearbyScanTest {
    private static final int RADIUS = MekmmWindGeneratorNearbyScan.CHUNK_RADIUS;

    @Test
    void emptyAreaIsClear() {
        assertFalse(scan(8, 64, 8, Set.of(chunk(0, 0)), Map.of()));
    }

    @Test
    void selfTileIsIgnored() {
        Map<Long, List<MekmmWindGeneratorNearbyScan.Candidate>> entities = Map.of(
                pack(0, 0),
                List.of(new MekmmWindGeneratorNearbyScan.Candidate(8, 64, 8, true, true))
        );
        assertFalse(scan(8, 64, 8, Set.of(chunk(0, 0)), entities));
    }

    @Test
    void otherMainBlockInSameChunkCounts() {
        Map<Long, List<MekmmWindGeneratorNearbyScan.Candidate>> entities = Map.of(
                pack(0, 0),
                List.of(
                        new MekmmWindGeneratorNearbyScan.Candidate(8, 64, 8, true, true),
                        new MekmmWindGeneratorNearbyScan.Candidate(10, 80, 10, true, true)
                )
        );
        assertTrue(scan(8, 64, 8, Set.of(chunk(0, 0)), entities));
    }

    @Test
    void boundingPartWithoutMainBlockDoesNotCount() {
        Map<Long, List<MekmmWindGeneratorNearbyScan.Candidate>> entities = Map.of(
                pack(0, 0),
                List.of(new MekmmWindGeneratorNearbyScan.Candidate(9, 70, 8, false, false))
        );
        assertFalse(scan(8, 64, 8, Set.of(chunk(0, 0)), entities));
    }

    @Test
    void generatorTileWithoutBlockMatchStillCounts() {
        Map<Long, List<MekmmWindGeneratorNearbyScan.Candidate>> entities = Map.of(
                pack(1, 0),
                List.of(new MekmmWindGeneratorNearbyScan.Candidate(20, 64, 8, false, true))
        );
        assertTrue(scan(8, 64, 8, Set.of(chunk(0, 0), chunk(1, 0)), entities));
    }

    @Test
    void outsideRadiusIsIgnored() {
        Map<Long, List<MekmmWindGeneratorNearbyScan.Candidate>> entities = Map.of(
                pack(3, 0),
                List.of(new MekmmWindGeneratorNearbyScan.Candidate(48, 64, 8, true, true))
        );
        assertFalse(scan(8, 64, 8, Set.of(chunk(0, 0), chunk(3, 0)), entities));
    }

    @Test
    void unloadedChunkIsNotQueried() {
        Map<Long, List<MekmmWindGeneratorNearbyScan.Candidate>> entities = new HashMap<>();
        entities.put(pack(1, 0), List.of(new MekmmWindGeneratorNearbyScan.Candidate(20, 64, 8, true, true)));
        assertFalse(scan(8, 64, 8, Set.of(chunk(0, 0)), entities));
    }

    @Test
    void edgeOfRadiusIsIncluded() {
        Map<Long, List<MekmmWindGeneratorNearbyScan.Candidate>> entities = Map.of(
                pack(2, 2),
                List.of(new MekmmWindGeneratorNearbyScan.Candidate(32, 64, 32, true, true))
        );
        assertTrue(scan(8, 64, 8, Set.of(chunk(0, 0), chunk(2, 2)), entities));
    }

    @Test
    void radiusMatchesOriginalConstant() {
        assertEquals(2, RADIUS);
        assertEquals(
                "com.jerry.meklg.common.tile.generator.TileEntityLargeWindGenerator",
                MekmmWindGeneratorNearbyScan.TILE_CLASS_NAME
        );
    }

    private static boolean scan(
            int x,
            int y,
            int z,
            Set<Long> loaded,
            Map<Long, List<MekmmWindGeneratorNearbyScan.Candidate>> entities
    ) {
        return MekmmWindGeneratorNearbyScan.hasNeighbor(
                x,
                y,
                z,
                RADIUS,
                (cx, cz) -> loaded.contains(pack(cx, cz)),
                (cx, cz) -> entities.getOrDefault(pack(cx, cz), List.of())
        );
    }

    private static long pack(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    private static long chunk(int cx, int cz) {
        return pack(cx, cz);
    }
}
