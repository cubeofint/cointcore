package com.mawlee.cointcore.industrialforegoingsouls;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Resolves Soul Surge facing from block state by property <em>name</em>
 * ({@value SoulSurgeTarget#FACING_PROPERTY_NAME}), not by
 * {@code BlockState.hasProperty(BlockStateProperties.FACING)} identity.
 * Titanium {@code RotatableBlock.FACING_ALL} is a different
 * {@code DirectionProperty} object that still serializes as {@code facing}.
 */
public final class SoulSurgeFacing {
    private SoulSurgeFacing() {
    }

    /**
     * Serialized facing value, or {@code null} if the state has no property
     * named {@value SoulSurgeTarget#FACING_PROPERTY_NAME}.
     */
    public static String name(BlockState state) {
        if (state == null) {
            return null;
        }
        for (Property<?> property : state.getProperties()) {
            if (!SoulSurgeTarget.isFacingPropertyName(property.getName())) {
                continue;
            }
            Comparable<?> value = state.getValue(property);
            if (value instanceof Direction direction) {
                return direction.getSerializedName();
            }
            return String.valueOf(value);
        }
        return null;
    }

    /**
     * Facing used for targeting; missing/unknown values become {@link Direction#UP}.
     */
    public static Direction direction(BlockState state) {
        Direction parsed = Direction.byName(SoulSurgeTarget.normalizeFacing(name(state)));
        return parsed != null ? parsed : Direction.UP;
    }

    /**
     * Attached cell, matching IF Souls: {@code pos.relative(facing.getOpposite())}.
     */
    public static BlockPos target(BlockPos surgePos, BlockState state) {
        return surgePos.relative(direction(state).getOpposite());
    }
}
