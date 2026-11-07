package com.mawlee.cointcore.refinedstorage.multicrafter;

import com.mawlee.cointcore.refinedstorage.multicrafter.MultiCrafterShape.Part;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MultiCrafterShapeTest {
    private final Map<Long, Part> world = new HashMap<>();

    private void put(int x, int y, int z, Part part) {
        world.put(MultiCrafterShape.pack(x, y, z), part);
    }

    /** A correct cuboid at (ox, oy, oz); the inside alternates CPU and storage. */
    private void build(int ox, int oy, int oz, int sx, int sy, int sz) {
        for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) {
            int border = (x == 0 || x == sx - 1 ? 1 : 0) + (y == 0 || y == sy - 1 ? 1 : 0) + (z == 0 || z == sz - 1 ? 1 : 0);
            Part part = border >= 2 ? Part.FRAME : border == 1 ? Part.VENT : (x + y + z) % 2 == 0 ? Part.CPU : Part.STORAGE;
            put(ox + x, oy + y, oz + z, part);
        }
    }

    private MultiCrafterShape.Result scan(int x, int y, int z) {
        return MultiCrafterShape.scan((a, b, c) -> world.get(MultiCrafterShape.pack(a, b, c)), x, y, z, 16);
    }

    @Test void packRoundTripsNegativeCoordinates() {
        for (int[] p : new int[][]{{0, 0, 0}, {-30_000_000, -64, 29_999_999}, {123, 319, -456}, {-1, -1, -1}}) {
            long packed = MultiCrafterShape.pack(p[0], p[1], p[2]);
            assertEquals(p[0], MultiCrafterShape.unpackX(packed));
            assertEquals(p[1], MultiCrafterShape.unpackY(packed));
            assertEquals(p[2], MultiCrafterShape.unpackZ(packed));
        }
    }

    @Test void minimalCrafterNeedsBothCpuAndStorage() {
        build(0, 0, 0, 3, 3, 3); // single inside block: (1,1,1) -> sum 3 -> storage
        var result = scan(0, 0, 0);
        assertEquals("no_cpu", result.error());
        put(1, 1, 1, Part.CPU);
        assertEquals("no_storage", scan(2, 2, 2).error());
    }

    @Test void validCrafterCountsInsideBlocksFromAnyStartingPart() {
        build(-5, 60, 10, 5, 4, 6);
        var result = scan(-3, 61, 12);
        assertTrue(result.valid(), result.error());
        assertEquals(3 * 2 * 4, result.cpus() + result.storages());
        assertTrue(result.cpus() > 0 && result.storages() > 0);
        assertEquals(5 * 4 * 6, result.parts().size());
        assertEquals(5, result.sizeX());
    }

    @Test void wrongBlocksAndHolesAreReportedWithTheirPosition() {
        build(0, 0, 0, 4, 4, 4);
        put(0, 1, 1, Part.FRAME);
        var result = scan(0, 0, 0);
        assertEquals("needs_vent", result.error());
        assertEquals(0, result.errorX());
        put(0, 1, 1, Part.VENT);
        put(1, 0, 0, Part.VENT);
        assertEquals("needs_frame", scan(0, 0, 0).error());
        put(1, 0, 0, Part.FRAME);
        put(1, 1, 1, Part.VENT);
        assertEquals("needs_inside", scan(0, 0, 0).error());
        put(1, 1, 1, Part.CPU);
        world.remove(MultiCrafterShape.pack(2, 3, 2));
        assertEquals("hole", scan(0, 0, 0).error());
        put(2, 3, 2, Part.VENT);
        world.remove(MultiCrafterShape.pack(2, 2, 1));
        assertTrue(scan(0, 0, 0).valid(), "empty space inside is allowed");
    }

    /** The biggest crafter with an empty inside: one CPU and one storage on the floor are enough. */
    @Test void hollowCrafterNeedsOnlyOneCpuAndOneStorage() {
        build(0, 0, 0, 16, 16, 16);
        for (int x = 1; x < 15; x++) for (int y = 1; y < 15; y++) for (int z = 1; z < 15; z++) {
            world.remove(MultiCrafterShape.pack(x, y, z));
        }
        assertEquals("no_cpu", scan(0, 0, 0).error());
        put(5, 1, 5, Part.CPU);
        put(6, 1, 5, Part.STORAGE);
        var result = scan(0, 0, 0);
        assertTrue(result.valid(), result.error());
        assertEquals(1, result.cpus());
        assertEquals(1, result.storages());
        assertTrue(scan(6, 1, 5).valid(), "the storage finds the same crafter");

        put(7, 7, 7, Part.CPU); // touches nothing
        var floating = scan(0, 0, 0);
        assertEquals("floating", floating.error());
        assertEquals(7, floating.errorY());
        assertEquals("floating", scan(7, 7, 7).error());
    }

    @Test void extraTouchingPartsAndOversizeAreRejected() {
        build(0, 0, 0, 3, 3, 4); // inside: (1,1,1) storage, (1,1,2) CPU
        assertTrue(scan(0, 0, 0).valid(), scan(0, 0, 0).error());
        put(3, 0, 0, Part.FRAME);
        assertFalse(scan(0, 0, 0).valid(), "a part sticking out grows the box and breaks its shape");
        world.clear();
        build(0, 0, 0, 3, 3, 17);
        assertEquals("too_big", scan(0, 0, 0).error());
        world.clear();
        build(0, 0, 0, 2, 3, 3);
        assertEquals("too_small", scan(0, 0, 0).error());
    }
}
