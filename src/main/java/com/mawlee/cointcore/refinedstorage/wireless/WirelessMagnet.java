package com.mawlee.cointcore.refinedstorage.wireless;

import com.mawlee.cointcore.CointCore;
import com.refinedmods.refinedstorage.common.support.AbstractBaseContainerMenu;
import com.ultramega.universalgrid.common.interfaces.MixinDisabledSlot;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.List;

/** Legacy item registration and recovery only. Wireless upgrades now provide the magnet. */
public final class WirelessMagnet {
    private static final String LEGACY_KEY = "cointcore_wireless_magnet";
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CointCore.MOD_ID);
    public static final DeferredItem<Item> CARD = ITEMS.register("wireless_magnet_card",
            () -> new Item(new Item.Properties().stacksTo(1)) {
                @Override public void appendHoverText(ItemStack stack, TooltipContext context,
                                                       List<Component> lines, TooltipFlag flags) {
                    super.appendHoverText(stack, context, lines, flags);
                    lines.add(Component.translatable("tooltip.cointcore.magnet.retired"));
                }
            });

    private WirelessMagnet() { }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        NeoForge.EVENT_BUS.addListener(WirelessMagnet::recoverOnOpen);
    }

    private static void recoverOnOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getContainer() instanceof AbstractBaseContainerMenu menu)) return;
        var reference = ((MixinDisabledSlot) menu).universalgrid$getDisabledSlot();
        if (reference == null) return;
        var device = reference.resolve(player).orElse(ItemStack.EMPTY);
        if (!WirelessPatternIntegration.isUniversalGrid(device)) return;
        if (!device.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(LEGACY_KEY)) return;
        var card = recoverCard(device, player.registryAccess());
        if (!card.isEmpty()) player.getInventory().placeItemBackInInventory(card);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    static ItemStack recoverCard(ItemStack device, HolderLookup.Provider registries) {
        var data = device.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!data.contains(LEGACY_KEY)) return ItemStack.EMPTY;
        var card = ItemStack.parseOptional(registries, data.getCompound(LEGACY_KEY).getCompound("card"));
        // Clear only our old state before giving the real item back. Other terminal data is untouched.
        CustomData.update(DataComponents.CUSTOM_DATA, device, tag -> tag.remove(LEGACY_KEY));
        return card;
    }
}
