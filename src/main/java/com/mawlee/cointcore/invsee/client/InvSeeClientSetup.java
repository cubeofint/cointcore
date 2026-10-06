package com.mawlee.cointcore.invsee.client;

import com.mawlee.cointcore.invsee.InvSeeMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class InvSeeClientSetup {
    private InvSeeClientSetup() {
    }

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(InvSeeMenus.PLAYER.get(), InvSeePlayerScreen::new);
        event.register(InvSeeMenus.ENDER.get(), InvSeeEnderScreen::new);
        event.register(InvSeeMenus.CURIOS.get(), InvSeeCuriosScreen::new);
        event.register(InvSeeMenus.ATTACHMENT.get(), InvSeeAttachmentScreen::new);
        event.register((net.minecraft.world.inventory.MenuType) InvSeeMenus.COSMETIC.get(), InvSeeCosmeticScreen::new);
        event.register((net.minecraft.world.inventory.MenuType) InvSeeMenus.POCKET.get(), InvSeePocketScreen::new);
        event.register((net.minecraft.world.inventory.MenuType) InvSeeMenus.BACKPACK.get(), InvSeeBackpackScreen::new);
    }
}
