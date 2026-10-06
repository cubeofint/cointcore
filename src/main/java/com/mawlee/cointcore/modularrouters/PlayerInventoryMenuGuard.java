package com.mawlee.cointcore.modularrouters;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Blocks Modular Routers Player Module extraction while a container GUI is open.
 * Pulling an inventory item (e.g. wireless terminal) mid-GUI causes classic item dupes.
 */
public final class PlayerInventoryMenuGuard {
    private PlayerInventoryMenuGuard() {
    }

    public static boolean blocksExtraction(Player player) {
        if (player == null || player.level().isClientSide) {
            return false;
        }

        AbstractContainerMenu open = player.containerMenu;
        return open != null && open != player.inventoryMenu;
    }
}
