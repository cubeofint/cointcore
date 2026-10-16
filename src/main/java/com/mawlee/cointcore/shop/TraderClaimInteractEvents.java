package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.config.TraderOffersConfig;
import com.mawlee.cointcore.ftb.FtbIntegration;
import com.mawlee.cointcore.lang.CointCoreMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opens trader GUIs inside foreign FTB Chunks claims. Only a non-sneak right-click
 * that would otherwise be blocked as {@code INTERACT_BLOCK} is allowed through;
 * break, place, wrench, sneak+item and other uses stay under claim protection.
 */
@EventBusSubscriber(modid = CointCore.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.DEDICATED_SERVER)
public final class TraderClaimInteractEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraderClaimInteractEvents.class);
    private static final Map<UUID, Integer> OPENED_AT_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> NOTIFIED_AT_TICK = new ConcurrentHashMap<>();

    private TraderClaimInteractEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRightClickBlockHighest(PlayerInteractEvent.RightClickBlock event) {
        tryOpenProtectedTrader(event, EventPriority.HIGHEST);
    }

    @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
    public static void onRightClickBlockHigh(PlayerInteractEvent.RightClickBlock event) {
        logCancelProbe(event, EventPriority.HIGH);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    public static void onRightClickBlockNormal(PlayerInteractEvent.RightClickBlock event) {
        logCancelProbe(event, EventPriority.NORMAL);
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    public static void onRightClickBlockLow(PlayerInteractEvent.RightClickBlock event) {
        logCancelProbe(event, EventPriority.LOW);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onRightClickBlockLowest(PlayerInteractEvent.RightClickBlock event) {
        tryOpenProtectedTrader(event, EventPriority.LOWEST);
    }

    private static void tryOpenProtectedTrader(PlayerInteractEvent.RightClickBlock event, EventPriority priority) {
        if (event.getSide().isClient()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        boolean trader = isTrader(state);
        boolean sneak = player.isShiftKeyDown();
        boolean mainHand = event.getHand() == InteractionHand.MAIN_HAND;
        boolean wasCanceledBefore = event.isCanceled();
        boolean canInteract = trader && FtbIntegration.canInteractBlock(player, level, pos);

        if (TraderOffersConfig.debugTraderClicks()) {
            logClick(event, player, pos, state, sneak, wasCanceledBefore, canInteract, false, priority);
        } else if (priority != EventPriority.HIGHEST && priority != EventPriority.LOWEST) {
            return;
        }

        if (!trader || sneak) {
            return;
        }
        if (canInteract) {
            return;
        }

        if (!mainHand) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        if (alreadyOpenedThisTick(player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        boolean opened = openTrader(player, level, pos, state);
        if (TraderOffersConfig.debugTraderClicks()) {
            logClick(event, player, pos, state, sneak, wasCanceledBefore, canInteract, opened, priority);
        }
        if (!opened) {
            return;
        }

        markOpenedThisTick(player);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static boolean openTrader(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        if (state.is(ShopBlocks.TRADER.get())) {
            return TraderBlock.openMenu(player, level, pos);
        }
        if (!(level.getBlockEntity(pos) instanceof PlayerTraderBlockEntity shop)) {
            return false;
        }
        if (!PlayerShopAccess.canUse(player)) {
            Integer lastNotify = NOTIFIED_AT_TICK.put(player.getUUID(), player.tickCount);
            if (lastNotify == null || lastNotify != player.tickCount) {
                player.sendSystemMessage(
                        CointCoreMessages.forPlayer(player, CointCoreMessages.PLAYER_SHOP_NO_USE)
                );
            }
            return false;
        }
        OptionalInt containerId = PlayerTraderMenus.openReturningId(player, shop, false);
        return containerId.isPresent();
    }

    private static boolean alreadyOpenedThisTick(ServerPlayer player) {
        Integer lastTick = OPENED_AT_TICK.get(player.getUUID());
        return lastTick != null && lastTick == player.tickCount;
    }

    private static void markOpenedThisTick(ServerPlayer player) {
        OPENED_AT_TICK.put(player.getUUID(), player.tickCount);
    }

    private static void logCancelProbe(PlayerInteractEvent.RightClickBlock event, EventPriority priority) {
        if (!TraderOffersConfig.debugTraderClicks() || event.getSide().isClient()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!isTrader(state)) {
            return;
        }
        LOGGER.info(
                "trader click probe priority={} canceled={} useBlock={} useItem={} player={} pos={}",
                priority,
                event.isCanceled(),
                event.getUseBlock(),
                event.getUseItem(),
                player.getGameProfile().getName(),
                event.getPos().toShortString()
        );
    }

    private static void logClick(
            PlayerInteractEvent.RightClickBlock event,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            boolean sneak,
            boolean wasCanceledBefore,
            boolean canInteract,
            boolean menuOpened,
            EventPriority priority
    ) {
        ItemStack held = player.getMainHandItem();
        LOGGER.info(
                "trader click player={} pos={} block={} hand={} sneak={} held={} side={} wasCanceledBefore={}"
                        + " useBlock={} useItem={} canInteractBlock={} menuOpened={} priority={}",
                player.getGameProfile().getName(),
                pos.toShortString(),
                BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                event.getHand(),
                sneak,
                held.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(held.getItem()),
                event.getSide(),
                wasCanceledBefore,
                event.getUseBlock(),
                event.getUseItem(),
                canInteract,
                menuOpened,
                priority
        );
    }

    private static boolean isTrader(BlockState state) {
        return state.is(ShopBlocks.TRADER.get()) || state.is(ShopBlocks.PLAYER_TRADER.get());
    }
}
