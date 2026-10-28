package com.mawlee.cointcore.radio;

import com.mawlee.cointcore.CointCore;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class CointRadioBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, CointCore.MOD_ID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, CointCore.MOD_ID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CointCore.MOD_ID);

    public static final DeferredHolder<Block, Block> COINT_RADIO =
            BLOCKS.register("coint_radio", CointRadioBlock::new);

    public static final DeferredHolder<Block, Block> COINT_SPEAKER =
            BLOCKS.register("coint_speaker", CointSpeakerBlock::new);

    public static final DeferredHolder<Item, Item> COINT_RADIO_ITEM =
            ITEMS.register(
                    "coint_radio",
                    () -> new CointRadioBlockItem(
                            COINT_RADIO.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredHolder<Item, Item> COINT_SPEAKER_ITEM =
            ITEMS.register(
                    "coint_speaker",
                    () -> new CointSpeakerBlockItem(
                            COINT_SPEAKER.get(),
                            new Item.Properties()
                    )
            );

    public static final DeferredHolder<Item, Item> COINT_TUNER_ITEM =
            ITEMS.register(
                    "coint_tuner",
                    () -> new CointTunerItem(new Item.Properties().stacksTo(1))
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CointRadioBlockEntity>> COINT_RADIO_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "coint_radio",
                    () -> BlockEntityType.Builder
                            .of(CointRadioBlockEntity::new, COINT_RADIO.get())
                            .build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CointSpeakerBlockEntity>> COINT_SPEAKER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register(
                    "coint_speaker",
                    () -> BlockEntityType.Builder
                            .of(CointSpeakerBlockEntity::new, COINT_SPEAKER.get())
                            .build(null)
            );

    private CointRadioBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(CointRadioBlocks::creative);
    }

    private static void creative(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(COINT_RADIO_ITEM.get());
            event.accept(COINT_SPEAKER_ITEM.get());
            event.accept(COINT_TUNER_ITEM.get());
        }
    }
}
