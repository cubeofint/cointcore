package com.mawlee.cointcore.refinedstorage.multicrafter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates a multiblock crafter, the same layout as RebornStorage's: a cuboid at least 3×3×3 whose edges are
 * frame blocks and whose faces are heat vents. The inside may be partly empty but must hold at least one CPU and one
 * pattern storage; every part inside has to touch the walls or another counted part.
 * Pure logic over a part lookup, so it can be unit-tested without a world.
 */
public final class MultiCrafterShape {
    public enum Part { FRAME, VENT, CPU, STORAGE }

    @FunctionalInterface
    public interface Lookup {
        /** The crafter part at the position, or null if something else is there. */
        Part partAt(int x, int y, int z);
    }

    /**
     * @param error  translation key suffix explaining why the structure is not valid, or null when it is
     * @param errorX position the error refers to (when there is one)
     * @param parts  every connected crafter block found, packed with {@link #pack}
     */
    public record Result(String error, int errorX, int errorY, int errorZ,
                         int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                         int cpus, int storages, List<Long> parts) {
        public boolean valid() {
            return error == null;
        }

        public int sizeX() { return maxX - minX + 1; }
        public int sizeY() { return maxY - minY + 1; }
        public int sizeZ() { return maxZ - minZ + 1; }
    }

    private static final int[][] DIRECTIONS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private MultiCrafterShape() {
    }

    public static long pack(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (long) y & 0xFFF;
    }

    public static int unpackX(long packed) { return (int) (packed >> 38); }
    public static int unpackY(long packed) { return (int) (packed << 52 >> 52); }
    public static int unpackZ(long packed) { return (int) (packed << 26 >> 38); }

    public static Result scan(Lookup lookup, int startX, int startY, int startZ, int maxSize) {
        int limit = maxSize * maxSize * maxSize;
        Set<Long> seen = new HashSet<>();
        List<Long> parts = new ArrayList<>();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        if (lookup.partAt(startX, startY, startZ) == null) {
            return error("not_a_part", startX, startY, startZ, parts);
        }
        queue.add(new int[]{startX, startY, startZ});
        seen.add(pack(startX, startY, startZ));
        int minX = startX, minY = startY, minZ = startZ, maxX = startX, maxY = startY, maxZ = startZ;
        boolean touchesWall = false;
        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            parts.add(pack(pos[0], pos[1], pos[2]));
            Part found = lookup.partAt(pos[0], pos[1], pos[2]);
            touchesWall |= found == Part.FRAME || found == Part.VENT;
            if (parts.size() > limit) {
                return error("too_big", pos[0], pos[1], pos[2], parts);
            }
            minX = Math.min(minX, pos[0]); maxX = Math.max(maxX, pos[0]);
            minY = Math.min(minY, pos[1]); maxY = Math.max(maxY, pos[1]);
            minZ = Math.min(minZ, pos[2]); maxZ = Math.max(maxZ, pos[2]);
            for (int[] d : DIRECTIONS) {
                int x = pos[0] + d[0], y = pos[1] + d[1], z = pos[2] + d[2];
                if (lookup.partAt(x, y, z) != null && seen.add(pack(x, y, z))) {
                    queue.add(new int[]{x, y, z});
                }
            }
        }
        if (!touchesWall) {
            // CPUs and storages left floating in the empty inside, not touching the walls or other parts.
            return error("floating", startX, startY, startZ, parts);
        }
        int sx = maxX - minX + 1, sy = maxY - minY + 1, sz = maxZ - minZ + 1;
        if (sx > maxSize || sy > maxSize || sz > maxSize) {
            return error("too_big", maxX, maxY, maxZ, parts);
        }
        if (sx < 3 || sy < 3 || sz < 3) {
            return error("too_small", minX, minY, minZ, parts);
        }
        int cpus = 0, storages = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Part part = lookup.partAt(x, y, z);
                    int onBorder = (x == minX || x == maxX ? 1 : 0) + (y == minY || y == maxY ? 1 : 0)
                            + (z == minZ || z == maxZ ? 1 : 0);
                    if (part == null) {
                        if (onBorder == 0) continue; // the inside may be partly empty
                        return error("hole", x, y, z, parts);
                    }
                    Part expected = onBorder >= 2 ? Part.FRAME : onBorder == 1 ? Part.VENT : null;
                    if (expected != null && part != expected) {
                        return error(expected == Part.FRAME ? "needs_frame" : "needs_vent", x, y, z, parts);
                    }
                    if (expected == null) {
                        if (part != Part.FRAME && part != Part.VENT && !seen.contains(pack(x, y, z))) {
                            return error("floating", x, y, z, parts);
                        }
                        if (part == Part.CPU) cpus++;
                        else if (part == Part.STORAGE) storages++;
                        else return error("needs_inside", x, y, z, parts);
                    }
                }
            }
        }
        if (cpus == 0) return error("no_cpu", minX, minY, minZ, parts);
        if (storages == 0) return error("no_storage", minX, minY, minZ, parts);
        return new Result(null, 0, 0, 0, minX, minY, minZ, maxX, maxY, maxZ, cpus, storages, parts);
    }

    private static Result error(String error, int x, int y, int z, List<Long> parts) {
        return new Result(error, x, y, z, 0, 0, 0, 0, 0, 0, 0, 0, parts);
    }
}
