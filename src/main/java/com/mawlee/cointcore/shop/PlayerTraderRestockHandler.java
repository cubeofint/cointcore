package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.config.TraderOffersConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.slf4j.Logger;

/**
 * Insert-only capability on {@link PlayerTraderBlockEntity}: pipes restock the
 * owner's matching global-market lot. Extract is always empty.
 */
public final class PlayerTraderRestockHandler implements IItemHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final PlayerTraderBlockEntity shop;

    public PlayerTraderRestockHandler(PlayerTraderBlockEntity shop) {
        this.shop = shop;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != 0 || stack == null || stack.isEmpty()) {
            return stack == null ? ItemStack.EMPTY : stack;
        }
        int accepted = GlobalMarketService.tryRestock(shop, stack, simulate);
        if (TraderOffersConfig.playerShopRestockDebug()) {
            LOGGER.info(
                    "player_trader insert slot={} simulate={} count={} accepted={} leftover={} owner={} pos={}",
                    slot,
                    simulate,
                    stack.getCount(),
                    accepted,
                    Math.max(0, stack.getCount() - accepted),
                    shop.ownerId(),
                    shop.getBlockPos()
            );
        }
        if (accepted <= 0) {
            return stack;
        }
        if (accepted >= stack.getCount()) {
            return ItemStack.EMPTY;
        }
        ItemStack leftover = stack.copy();
        leftover.shrink(accepted);
        return leftover;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == 0 && stack != null && !stack.isEmpty()
                && GlobalMarketService.tryRestock(shop, stack, true) > 0;
    }
}
