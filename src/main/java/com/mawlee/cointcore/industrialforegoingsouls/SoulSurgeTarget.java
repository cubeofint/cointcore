package com.mawlee.cointcore.industrialforegoingsouls;

import java.util.Locale;

/**
 * Pure target-selection rules for Industrial Foregoing Souls Soul Surge.
 * <p>
 * IF Souls stores Titanium {@code RotatableBlock.FACING_ALL}
 * ({@code BlockStateProperties.FACING}) as the clicked face — the pipe/back
 * side. The nozzle points at {@code facing.getOpposite()}, so the attached
 * machine is {@code surgePos.relative(facing.opposite)}.
 * <p>
 * If the block has no facing, this falls back to {@code up} (target = block
 * below), matching a surge sitting on top of a machine.
 */
public final class SoulSurgeTarget {
    public static final String SOUL_SURGE_BLOCK_ID = "industrialforegoingsouls:soul_surge";
    public static final String FALLBACK_FACING = "up";

    public record Offset(int x, int y, int z) {
    }

    private SoulSurgeTarget() {
    }

    /**
     * Normalizes a FACING property name. Unknown or missing values become
     * {@link #FALLBACK_FACING}.
     */
    public static String normalizeFacing(String facingName) {
        if (facingName == null || facingName.isBlank()) {
            return FALLBACK_FACING;
        }
        String facing = facingName.trim().toLowerCase(Locale.ROOT);
        return switch (facing) {
            case "down", "up", "north", "south", "west", "east" -> facing;
            default -> FALLBACK_FACING;
        };
    }

    /**
     * Offset from the surge block to the single attached cell
     * ({@code facing.opposite}).
     */
    public static Offset attachedOffset(String facingName) {
        return switch (normalizeFacing(facingName)) {
            case "down" -> new Offset(0, 1, 0);
            case "up" -> new Offset(0, -1, 0);
            case "north" -> new Offset(0, 0, 1);
            case "south" -> new Offset(0, 0, -1);
            case "west" -> new Offset(1, 0, 0);
            case "east" -> new Offset(-1, 0, 0);
            default -> attachedOffset(FALLBACK_FACING);
        };
    }

    public static int[] attachedCoordinates(int x, int y, int z, String facingName) {
        Offset offset = attachedOffset(facingName);
        return new int[]{x + offset.x(), y + offset.y(), z + offset.z()};
    }

    public static boolean isSoulSurgeId(String blockId) {
        return SOUL_SURGE_BLOCK_ID.equals(blockId);
    }

    /**
     * Air and blocks without a block entity do nothing. Another soul surge is
     * never a valid target (prevents chaining).
     */
    public static boolean shouldAccelerate(boolean isAir, boolean hasBlockEntity, String targetBlockId) {
        return !isAir && hasBlockEntity && !isSoulSurgeId(targetBlockId);
    }
}
