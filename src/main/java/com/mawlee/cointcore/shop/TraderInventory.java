package com.mawlee.cointcore.shop;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

final class TraderInventory {
    private TraderInventory() {
    }

    static boolean canFullyFit(Inventory inventory, ItemStack toAdd) {
        if (toAdd.isEmpty()) {
            return true;
        }
        ItemStack remaining = toAdd.copy();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            ItemStack existing = inventory.items.get(slot);
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

    static int countMatching(Inventory inventory, ItemStack sample) {
        int total = 0;
        for (ItemStack stack : inventory.items) {
            if (ItemStack.isSameItemSameComponents(stack, sample)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    static boolean removeMatching(Inventory inventory, ItemStack sample, int amount) {
        if (countMatching(inventory, sample) < amount) {
            return false;
        }
        int remaining = amount;
        for (int slot = 0; slot < inventory.items.size() && remaining > 0; slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (!ItemStack.isSameItemSameComponents(stack, sample)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        return remaining == 0;
    }

    static void giveOrFail(Inventory inventory, ItemStack goods) {
        inventory.add(goods);
        if (!goods.isEmpty()) {
            inventory.player.drop(goods, false);
        }
    }
}
