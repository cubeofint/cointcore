package com.mawlee.cointcore.refinedstorage.wireless;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.shop.jei.CointJeiPlugin;
import com.refinedmods.refinedstorage.common.grid.screen.AbstractGridScreen;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CointCore.MOD_ID, value = Dist.CLIENT)
public final class WirelessPickClient {
    private WirelessPickClient() { }

    private static boolean enabled() {
        return ModList.get().isLoaded("refinedstorage") && ModList.get().isLoaded("universalgrid");
    }

    private static boolean pick(ItemStack stack, boolean cursor) {
        var player = Minecraft.getInstance().player;
        if (stack.isEmpty() || player == null || WirelessTerminalActions.terminals(player).isEmpty()) return false;
        PacketDistributor.sendToServer(new WirelessTerminalActions.PickPayload(stack.copyWithCount(1), cursor));
        return true;
    }

    @SubscribeEvent
    public static void inWorld(InputEvent.InteractionKeyMappingTriggered event) {
        if (!enabled() || !event.isPickBlock()) return;
        var minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.level == null || minecraft.player == null
                || !(minecraft.hitResult instanceof BlockHitResult hit)) return;
        ItemStack stack = minecraft.level.getBlockState(hit.getBlockPos()).getCloneItemStack(hit, minecraft.level,
                hit.getBlockPos(), minecraft.player);
        if (pick(stack, false)) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void inScreen(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!enabled() || !Minecraft.getInstance().options.keyPickItem.matchesMouse(event.getButton())) return;
        ItemStack stack = ItemStack.EMPTY;
        if (ModList.get().isLoaded("jei")) stack = CointJeiPlugin.itemUnderMouse();
        if (stack.isEmpty() && event.getScreen() instanceof AbstractGridScreen<?> grid) {
            var resource = grid.getCurrentGridResource();
            if (resource != null && resource.getResourceForRecipeMods() instanceof ItemResource item) stack = item.toItemStack();
        }
        if (stack.isEmpty() && event.getScreen() instanceof AbstractContainerScreen<?> container) {
            var slot = container.getSlotUnderMouse();
            if (slot != null && slot.hasItem()) stack = slot.getItem();
        }
        if (pick(stack, true)) event.setCanceled(true);
    }
}
