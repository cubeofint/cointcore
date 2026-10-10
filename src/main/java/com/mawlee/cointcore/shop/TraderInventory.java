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

    static int spaceFor(Inventory inventory, ItemStack sample) {
        if (sample.isEmpty()) {
            return 0;
        }
        int space = 0;
        int max = sample.getMaxStackSize();
        for (ItemStack existing : inventory.items) {
            if (existing.isEmpty()) {
                space += max;
            } else if (ItemStack.isSameItemSameComponents(existing, sample)) {
                space += Math.max(0, max - existing.getCount());
            }
        }
        return space;
    }

    static int countMatching(Inventory inventory, ItemStack sample) {
        long total = countMatchingLong(inventory, sample);
        return total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
    }

    static long countMatchingLong(Inventory inventory, ItemStack sample) {
        long total = 0L;
        for (ItemStack stack : inventory.items) {
            if (ItemStack.isSameItemSameComponents(stack, sample)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    static boolean removeMatching(Inventory inventory, ItemStack sample, int amount) {
        return removeMatching(inventory, sample, (long) amount);
    }

    static boolean removeMatching(Inventory inventory, ItemStack sample, long amount) {
        if (amount <= 0L) {
            return true;
        }
        if (countMatchingLong(inventory, sample) < amount) {
            return false;
        }
        long remaining = amount;
        for (int slot = 0; slot < inventory.items.size() && remaining > 0L; slot++) {
            ItemStack stack = inventory.items.get(slot);
            if (!ItemStack.isSameItemSameComponents(stack, sample)) {
                continue;
            }
            int take = (int) Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        return remaining == 0L;
    }

    static void giveOrFail(Inventory inventory, ItemStack goods) {
        inventory.add(goods);
        if (!goods.isEmpty()) {
            inventory.player.drop(goods, false);
        }
    }

    static void giveAmount(Inventory inventory, ItemStack template, long amount) {
        if (template == null || template.isEmpty() || amount <= 0L) {
            return;
        }
        int max = Math.max(1, template.getMaxStackSize());
        long remaining = amount;
        while (remaining > 0L) {
            int chunk = (int) Math.min(max, remaining);
            ItemStack stack = template.copy();
            stack.setCount(chunk);
            giveOrFail(inventory, stack);
            remaining -= chunk;
        }
    }
}
