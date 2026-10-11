package com.mawlee.cointcore.speedrunner;

import com.mawlee.cointcore.CointCore;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID)
public final class SpeedrunnerEvents {
    private SpeedrunnerEvents() {
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpeedrunnerService.onCrafted(player, event.getCrafting());
        }
    }
}
