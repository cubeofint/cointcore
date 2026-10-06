package com.mawlee.cointcore.invsee;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class InvSeeItemStacks {
    private InvSeeItemStacks() {
    }

    public static String describe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return InvSeeItemLabels.describe(null, 0);
        }
        return InvSeeItemLabels.describe(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount());
    }

    public static boolean same(ItemStack left, ItemStack right) {
        return ItemStack.matches(left, right);
    }
}
