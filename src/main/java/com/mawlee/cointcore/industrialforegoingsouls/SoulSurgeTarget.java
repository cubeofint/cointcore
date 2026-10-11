package com.mawlee.cointcore.industrialforegoingsouls;

import java.util.Locale;

/**
 * Pure target-selection rules for Industrial Foregoing Souls Soul Surge.
 * <p>
 * IF Souls stores Titanium {@code RotatableBlock.FACING_ALL} — a
 * {@code DirectionProperty} named {@value #FACING_PROPERTY_NAME}, but not the
 * same object as {@code BlockStateProperties.FACING}. Compare properties by
 * name, never by identity. The nozzle points at {@code facing.getOpposite()},
 * so the attached machine is {@code surgePos.relative(facing.opposite)}.
 * <p>
 * If the block has no facing, this falls back to {@code up} (target = block
 * below), matching a surge sitting on top of a machine.
 */
public final class SoulSurgeTarget {
    public static final String SOUL_SURGE_BLOCK_ID = "industrialforegoingsouls:soul_surge";
    public static final String FACING_PROPERTY_NAME = "facing";
    public static final String FALLBACK_FACING = "up";

    public record Offset(int x, int y, int z) {
    }

    public record NamedProperty(String name, String value) {
    }

    private SoulSurgeTarget() {
    }

    public static boolean isFacingPropertyName(String propertyName) {
        return FACING_PROPERTY_NAME.equals(propertyName);
    }

    /**
     * Reads the serialized facing from the first property named
     * {@value #FACING_PROPERTY_NAME}. Identity of the property object is
     * ignored (Titanium {@code FACING_ALL} vs vanilla {@code FACING}).
     */
    public static String facingFromProperties(Iterable<NamedProperty> properties) {
        if (properties == null) {
            return null;
        }
        for (NamedProperty property : properties) {
            if (property != null && isFacingPropertyName(property.name())) {
                return property.value();
            }
        }
        return null;
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
     * Unit step of {@code facing}, matching {@code Direction.getNormal()}.
     */
    public static Offset facingStep(String facingName) {
        return switch (normalizeFacing(facingName)) {
            case "down" -> new Offset(0, -1, 0);
            case "up" -> new Offset(0, 1, 0);
            case "north" -> new Offset(0, 0, -1);
            case "south" -> new Offset(0, 0, 1);
            case "west" -> new Offset(-1, 0, 0);
            case "east" -> new Offset(1, 0, 0);
            default -> facingStep(FALLBACK_FACING);
        };
    }

    /**
     * Offset from the surge to the attached cell:
     * {@code pos.relative(facing.getOpposite())} == {@code pos - facing.normal}.
     */
    public static Offset attachedOffset(String facingName) {
        Offset step = facingStep(facingName);
        return new Offset(-step.x(), -step.y(), -step.z());
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
