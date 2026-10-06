package com.mawlee.cointcore.modularrouters;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Delegating handler that refuses {@link #extractItem} while the player has a container open.
 */
public final class GuardedPlayerItemHandler implements IItemHandler {
    private final Player player;
    private final IItemHandler delegate;

    public GuardedPlayerItemHandler(Player player, IItemHandler delegate) {
        this.player = player;
        this.delegate = delegate;
    }

    @Override
    public int getSlots() {
        return delegate.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return delegate.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return delegate.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (PlayerInventoryMenuGuard.blocksExtraction(player)) {
            return ItemStack.EMPTY;
        }
        return delegate.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return delegate.isItemValid(slot, stack);
    }
}
