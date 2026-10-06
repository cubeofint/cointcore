package com.mawlee.cointcore.tickaccel;

import com.mawlee.cointcore.config.TickAccelerationDenyConfig;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared deny for tick accelerators (Time Wand, Soul Surge gate).
 * Rules live in {@link TickAccelerationDenyConfig} ({@code config/cointcore/tick-acceleration-deny.json}).
 */
public final class TickAccelerationDeny {
    private TickAccelerationDeny() {
    }

    public static boolean isDenied(BlockState state) {
        return TickAccelerationDenyConfig.isDenied(state);
    }

    public static boolean blockFakePlayers() {
        return TickAccelerationDenyConfig.blockFakePlayers();
    }
}
