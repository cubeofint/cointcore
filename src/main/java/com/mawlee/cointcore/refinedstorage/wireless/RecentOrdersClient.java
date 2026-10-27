package com.mawlee.cointcore.refinedstorage.wireless;

import com.mawlee.cointcore.CointCore;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.grid.screen.AbstractGridScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, value = Dist.CLIENT)
public final class RecentOrdersClient {
    public static final RequestHistory<ResourceKey> HISTORY = new RequestHistory<>(128);
    private RecentOrdersClient() { }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { HISTORY.clear(); }

    @SubscribeEvent
    public static void removePin(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!ModList.get().isLoaded("refinedstorage") || !ModList.get().isLoaded("universalgrid")) return;
        if (event.getButton() != 1 || !Screen.hasShiftDown()
                || !(event.getScreen() instanceof AbstractGridScreen<?> screen)) return;
        if (screen.getCurrentGridResource() instanceof RequestedGridResource resource) {
            if (resource.key() != null) HISTORY.forget(resource.key());
            event.setCanceled(true);
        }
    }
}
