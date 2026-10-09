package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ShopBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CointCore.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlayerTraderBlockEntity>> PLAYER_TRADER =
            REGISTER.register(
                    "player_trader",
                    () -> BlockEntityType.Builder.of(PlayerTraderBlockEntity::new, ShopBlocks.PLAYER_TRADER.get())
                            .build(null)
            );

    private ShopBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
