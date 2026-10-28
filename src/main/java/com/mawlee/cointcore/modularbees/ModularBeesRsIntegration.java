package com.mawlee.cointcore.modularbees;

import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import java.util.Set;

public final class ModularBeesRsIntegration {
    private static final Set<String> HATCHES = Set.of("me_beehive_export", "me_centrifuge_export", "me_centrifuge_import");
    private ModularBeesRsIntegration() { }

    public static void register(IEventBus bus) {
        bus.addListener(ModularBeesRsIntegration::capabilities);
        NeoForge.EVENT_BUS.addListener(ModularBeesRsIntegration::tooltip);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        Block[] blocks = HATCHES.stream().map(name -> BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("modularbees", name))).toArray(Block[]::new);
        event.registerBlock(RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
                (level, pos, state, blockEntity, side) -> blockEntity instanceof RsHatchHost host
                        ? host.cointcore$rsHatch().provider(side) : null, blocks);
    }

    private static void tooltip(ItemTooltipEvent event) {
        var id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (id.getNamespace().equals("modularbees") && HATCHES.contains(id.getPath())) {
            event.getToolTip().add(Component.translatable("tooltip.cointcore.modularbees_rs.connection"));
            event.getToolTip().add(Component.translatable("tooltip.cointcore.modularbees_rs.schedule"));
        }
    }
}
