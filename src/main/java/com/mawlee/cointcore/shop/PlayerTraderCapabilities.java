package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.slf4j.Logger;

/**
 * Registers insert-only item handlers on player vending machines for every side,
 * including a {@code null} side. Hoppers, EnderIO conduits and AE2/RS export buses
 * all query {@link Capabilities.ItemHandler#BLOCK}.
 */
@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class PlayerTraderCapabilities {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PlayerTraderCapabilities() {
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, blockEntity, side) -> handler(blockEntity),
                ShopBlocks.PLAYER_TRADER.get()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ShopBlockEntities.PLAYER_TRADER.get(),
                (blockEntity, side) -> handler(blockEntity, side)
        );
        LOGGER.info(
                "Registered {} item handler on {} (block + block entity, all sides including null)",
                Capabilities.ItemHandler.BLOCK.name(),
                ShopBlockEntities.PLAYER_TRADER.getId()
        );
    }

    private static IItemHandler handler(Object blockEntity) {
        return handler(blockEntity, null);
    }

    private static IItemHandler handler(Object blockEntity, Direction side) {
        if (blockEntity instanceof PlayerTraderBlockEntity shop) {
            return shop.restockHandler();
        }
        return null;
    }
}
