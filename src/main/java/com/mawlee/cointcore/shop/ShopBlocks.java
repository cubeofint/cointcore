package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ShopBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CointCore.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CointCore.MOD_ID);

    public static final DeferredBlock<TraderBlock> TRADER = BLOCKS.register(
            "trader",
            () -> new TraderBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
    );

    public static final DeferredItem<BlockItem> TRADER_ITEM = ITEMS.registerSimpleBlockItem(TRADER);

    public static final DeferredBlock<PlayerTraderBlock> PLAYER_TRADER = BLOCKS.register(
            "player_trader",
            () -> new PlayerTraderBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 3600000.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
    );

    public static final DeferredItem<BlockItem> PLAYER_TRADER_ITEM = ITEMS.registerSimpleBlockItem(PLAYER_TRADER);

    private ShopBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ShopBlockEntities.register(modEventBus);
        modEventBus.addListener(ShopBlocks::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(TRADER_ITEM);
            event.accept(PLAYER_TRADER_ITEM);
        }
    }
}
