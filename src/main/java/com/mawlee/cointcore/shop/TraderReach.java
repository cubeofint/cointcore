package com.mawlee.cointcore.shop;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.Block;

/**
 * Menu reach checks that only require the trader block to still exist within
 * interaction distance. Claim permissions are intentionally not consulted so
 * a customer can keep trading after opening the GUI in a foreign claim.
 */
final class TraderReach {
    private static final double MAX_DISTANCE_SQR = 64.0d;

    private TraderReach() {
    }

    static boolean stillValid(ContainerLevelAccess access, Player player, Block block) {
        return access.evaluate(
                (level, pos) -> level.getBlockState(pos).is(block)
                        && player.distanceToSqr(
                                pos.getX() + 0.5d,
                                pos.getY() + 0.5d,
                                pos.getZ() + 0.5d
                        ) <= MAX_DISTANCE_SQR,
                true
        );
    }
}
