package com.mawlee.cointcore.advancedae;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.world.Container;

import java.util.List;

public final class AutoStockInventoryCheck {
    private AutoStockInventoryCheck() { }

    /** Mirrors AdvancedAE's item-only matching, including armor/offhand and ignoring non-item filters. */
    public static boolean isBalanced(List<GenericStack> filter, Container inventory) {
        for (var target : filter) {
            if (target.what() instanceof AEItemKey key) {
                int current = 0;
                for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                    var stack = inventory.getItem(slot);
                    if (key.is(stack.getItem())) current += stack.getCount();
                }
                // Any mismatch falls back to the complete original method. In particular, do not skip
                // surplus deposits: duplicate filter entries may depend on earlier transfers in the loop.
                if (target.amount() != current) return false;
            }
        }
        return true;
    }
}
