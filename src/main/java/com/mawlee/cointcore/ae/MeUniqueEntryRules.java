package com.mawlee.cointcore.ae;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Rules for ME entries that are likely unique singletons (one key, count 1),
 * as opposed to bulk items like stacked potions that merge in AE2.
 */
public final class MeUniqueEntryRules {
    private MeUniqueEntryRules() {
    }

    public static boolean isCandidateItemType(Item item) {
        if (item == null || item == Items.AIR) {
            return false;
        }
        if (MeUniqueFilterConfig.isExcludedItem(item)) {
            return false;
        }
        return item.getDefaultInstance().getMaxStackSize() <= 1;
    }

    public static boolean isUniqueTerminalEntry(long storedAmount, ItemStack stack) {
        if (storedAmount != 1L || stack.isEmpty()) {
            return false;
        }
        return isCandidateItemType(stack.getItem());
    }

    /**
     * Inverse of {@link #isUniqueTerminalEntry}: entries where many non-stackable items collapsed
     * into a single {@code AEItemKey}, which is what makes a base "heavy" to store and sync.
     */
    public static boolean isMergedTerminalEntry(long storedAmount, ItemStack stack) {
        if (stack.isEmpty() || storedAmount < MeUniqueFilterConfig.getMergedMinAmount()) {
            return false;
        }
        return isCandidateItemType(stack.getItem());
    }
}
