package com.mawlee.cointcore.shop;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Stock helpers for the player vending machine. Matches items including components.
 */
final class ShopContainers {
    private ShopContainers() {
    }

    static boolean canFullyFit(Container container, ItemStack toAdd) {
        if (toAdd.isEmpty()) {
            return true;
        }
        ItemStack remaining = toAdd.copy();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack existing = container.getItem(slot);
            if (existing.isEmpty()) {
                int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                remaining.shrink(moved);
            } else if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    remaining.shrink(Math.min(space, remaining.getCount()));
                }
            }
            if (remaining.isEmpty()) {
                return true;
            }
        }
        return remaining.isEmpty();
    }

    static int spaceFor(Container container, ItemStack sample) {
        if (sample.isEmpty()) {
            return 0;
        }
        int space = 0;
        int max = sample.getMaxStackSize();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack existing = container.getItem(slot);
            if (existing.isEmpty()) {
                space += max;
            } else if (ItemStack.isSameItemSameComponents(existing, sample)) {
                space += Math.max(0, max - existing.getCount());
            }
        }
        return space;
    }

    static int countMatching(Container container, ItemStack sample) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, sample)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    static boolean removeMatching(Container container, ItemStack sample, int amount) {
        if (countMatching(container, sample) < amount) {
            return false;
        }
        int remaining = amount;
        for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = container.getItem(slot);
            if (!ItemStack.isSameItemSameComponents(stack, sample)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
            container.setChanged();
        }
        return remaining == 0;
    }

    static boolean insert(Container container, ItemStack goods) {
        if (goods.isEmpty()) {
            return true;
        }
        if (!canFullyFit(container, goods)) {
            return false;
        }
        ItemStack remaining = goods.copy();
        for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack existing = container.getItem(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, remaining)) {
                continue;
            }
            int space = existing.getMaxStackSize() - existing.getCount();
            if (space <= 0) {
                continue;
            }
            int moved = Math.min(space, remaining.getCount());
            existing.grow(moved);
            remaining.shrink(moved);
            container.setChanged();
        }
        for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            if (!container.getItem(slot).isEmpty()) {
                continue;
            }
            int moved = Math.min(remaining.getMaxStackSize(), remaining.getCount());
            container.setItem(slot, remaining.copyWithCount(moved));
            remaining.shrink(moved);
        }
        return remaining.isEmpty();
    }
}
