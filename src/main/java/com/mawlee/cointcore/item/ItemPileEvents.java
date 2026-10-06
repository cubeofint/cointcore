package com.mawlee.cointcore.item;

import com.mawlee.cointcore.CointCore;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class ItemPileEvents {
    private ItemPileEvents() {
    }

    /** Fires synchronously inside {@code discard()}, before vanilla pickup restores the stack count. */
    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity item && !event.getLevel().isClientSide()) {
            ItemPiles.onRemoved(item);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MassDropGuard.flush();
        ItemPiles.flush();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MassDropGuard.flush();
        ItemPiles.flush();
        ItemPiles.clear();
    }
}
