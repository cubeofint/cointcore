package com.mawlee.cointcore.refinedstorage;

import com.mawlee.cointcore.CointCore;
import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi;
import com.refinedmods.refinedstorage.common.support.tooltip.ResourceClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class InfinityDiskClientSetup {
    private InfinityDiskClientSetup() {
    }

    @SubscribeEvent
    public static void registerColors(RegisterColorHandlersEvent.Item event) {
        if (!ModList.get().isLoaded("refinedstorage")) {
            return;
        }
        for (var disk : InfinityDisks.all()) {
            var item = disk.item().get();
            event.register((stack, tintIndex) -> insetColor(item.color(), tintIndex), item);
        }
    }

    private static int insetColor(int color, int tintIndex) {
        if (tintIndex < 0 || tintIndex > 2) {
            return -1;
        }
        float factor = tintIndex == 1 ? 1.2F : tintIndex == 2 ? 0.72F : 1.0F;
        int red = Math.min(255, Math.round(((color >> 16) & 255) * factor));
        int green = Math.min(255, Math.round(((color >> 8) & 255) * factor));
        int blue = Math.min(255, Math.round((color & 255) * factor));
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    @SubscribeEvent
    public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        if (ModList.get().isLoaded("refinedstorage")) {
            event.register(InfinityDiskTooltip.class, tooltip -> new ResourceClientTooltipComponent(tooltip.resource()));
        }
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        if (!ModList.get().isLoaded("refinedstorage")) {
            return;
        }
        // RS uses separate models for disks visible inside the drive.
        var itemModel = ResourceLocation.fromNamespaceAndPath("refinedstorage", "block/disk/disk");
        var fluidModel = ResourceLocation.fromNamespaceAndPath("refinedstorage", "block/disk/fluid_disk");
        event.register(ModelResourceLocation.standalone(itemModel));
        event.register(ModelResourceLocation.standalone(fluidModel));
        var api = RefinedStorageClientApi.INSTANCE;
        for (var disk : InfinityDisks.all()) {
            api.registerDiskModel(disk.item().get(), disk.fluid() ? fluidModel : itemModel);
        }
    }
}
