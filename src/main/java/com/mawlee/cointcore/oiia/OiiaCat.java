package com.mawlee.cointcore.oiia;

import com.mawlee.cointcore.CointCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** OIIA cat: a decoration that spins to its song. Item places it, a hit takes it back, right-click toggles the music. */
public final class OiiaCat {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CointCore.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, CointCore.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, CointCore.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<OiiaCatEntity>> ENTITY = ENTITIES.register(
            "oiia_cat",
            () -> EntityType.Builder.<OiiaCatEntity>of(OiiaCatEntity::new, MobCategory.MISC)
                    .sized(1.0F, 0.8F)
                    .clientTrackingRange(8)
                    .updateInterval(20)
                    .build("oiia_cat")
    );

    public static final DeferredHolder<Item, Item> ITEM =
            ITEMS.register("oiia_cat", () -> new OiiaCatItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<SoundEvent, SoundEvent> SOUND = SOUNDS.register(
            "oiia_cat",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(CointCore.MOD_ID, "oiia_cat"))
    );

    private OiiaCat() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        SOUNDS.register(modEventBus);
        modEventBus.addListener(OiiaCat::creative);
    }

    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ITEM.get());
        }
    }
}
