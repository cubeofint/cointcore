package com.mawlee.cointcore.shop;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class PlayerShopEvents {
    private PlayerShopEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerShopEvents::onBreak);
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (!event.getState().is(ShopBlocks.PLAYER_TRADER.get())) {
            return;
        }
        Player player = event.getPlayer();
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof PlayerTraderBlockEntity shop)) {
            event.setCanceled(true);
            return;
        }
        if (!shop.canManage(player)) {
            event.setCanceled(true);
        }
    }
}
