package com.mawlee.cointcore.shop;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Registers insert-only item handlers on player vending machines for every side.
 */
public final class PlayerTraderCapabilities {
    private PlayerTraderCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ShopBlockEntities.PLAYER_TRADER.get(),
                (blockEntity, side) -> blockEntity.restockHandler()
        );
    }
}
