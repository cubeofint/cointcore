package com.mawlee.cointcore.refinedstorage.multicrafter;

import com.mawlee.cointcore.CointCore;
import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Registers the multiblock crafter (a port of RebornStorage's) for Refined Storage 2. */
public final class MultiCrafters {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, CointCore.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, CointCore.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CointCore.MOD_ID);

    private static final Map<MultiCrafterShape.Part, DeferredHolder<Block, MultiCrafterBlock>> BLOCK_BY_PART =
            new EnumMap<>(MultiCrafterShape.Part.class);
    private static final Map<MultiCrafterShape.Part, DeferredHolder<Item, BlockItem>> ITEM_BY_PART =
            new EnumMap<>(MultiCrafterShape.Part.class);

    static {
        for (MultiCrafterShape.Part part : MultiCrafterShape.Part.values()) {
            String name = "multicrafter_" + part.name().toLowerCase(Locale.ROOT);
            DeferredHolder<Block, MultiCrafterBlock> block = BLOCKS.register(name, () -> new MultiCrafterBlock(part));
            BLOCK_BY_PART.put(part, block);
            ITEM_BY_PART.put(part, ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties())));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MultiCrafterPartBlockEntity>> PART_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("multicrafter_part", () -> BlockEntityType.Builder.of(MultiCrafterPartBlockEntity::new,
                    BLOCK_BY_PART.get(MultiCrafterShape.Part.FRAME).get(),
                    BLOCK_BY_PART.get(MultiCrafterShape.Part.VENT).get(),
                    BLOCK_BY_PART.get(MultiCrafterShape.Part.CPU).get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MultiCrafterStorageBlockEntity>> STORAGE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("multicrafter_storage", () -> BlockEntityType.Builder.of(MultiCrafterStorageBlockEntity::new,
                    BLOCK_BY_PART.get(MultiCrafterShape.Part.STORAGE).get()).build(null));

    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CointCore.MOD_ID);

    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<MultiCrafterMenu>> MENU =
            MENUS.register("multicrafter", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(MultiCrafterMenu::fromNetwork));

    private MultiCrafters() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            bus.addListener(com.mawlee.cointcore.refinedstorage.multicrafter.client.MultiCrafterScreen::register);
        }
        bus.addListener(MultiCrafters::capabilities);
        bus.addListener(MultiCrafters::creativeTab);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        var capability = RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability();
        event.registerBlockEntity(capability, PART_BLOCK_ENTITY.get(), (blockEntity, side) -> blockEntity.getContainerProvider());
        event.registerBlockEntity(capability, STORAGE_BLOCK_ENTITY.get(), (blockEntity, side) -> blockEntity.getContainerProvider());
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            ITEM_BY_PART.values().forEach(item -> event.accept(item.get()));
        }
    }
}
