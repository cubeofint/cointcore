package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.ftb.FtbIntegration;
import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Opens trader GUIs inside foreign FTB Chunks claims. Only a non-sneak right-click
 * that would otherwise be blocked as {@code INTERACT_BLOCK} is allowed through;
 * break, place, wrench, sneak+item and other uses stay under claim protection.
 */
@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class TraderClaimInteractEvents {
    private TraderClaimInteractEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide().isClient() || event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isShiftKeyDown() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!isTrader(state)) {
            return;
        }
        if (FtbIntegration.canInteractBlock(player, level, pos)) {
            return;
        }

        if (state.is(ShopBlocks.TRADER.get())) {
            TraderBlock.openMenu(player, level, pos);
        } else if (level.getBlockEntity(pos) instanceof PlayerTraderBlockEntity shop) {
            if (!PlayerShopAccess.canUse(player)) {
                player.sendSystemMessage(
                        CointCoreMessages.forPlayer(player, CointCoreMessages.PLAYER_SHOP_NO_USE)
                );
            } else {
                PlayerTraderMenus.open(player, shop, false);
            }
        } else {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static boolean isTrader(BlockState state) {
        return state.is(ShopBlocks.TRADER.get()) || state.is(ShopBlocks.PLAYER_TRADER.get());
    }
}
