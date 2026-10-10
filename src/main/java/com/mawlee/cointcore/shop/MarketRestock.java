package com.mawlee.cointcore.shop;

import net.minecraft.world.item.ItemStack;

/**
 * Pure restock arithmetic: how many items a lot can accept under the stock cap.
 */
public final class MarketRestock {
    private MarketRestock() {
    }

    public static int acceptCount(long stockItems, long maxStock, int incoming) {
        if (incoming <= 0 || maxStock <= 0L || stockItems >= maxStock) {
            return 0;
        }
        long room = maxStock - Math.max(0L, stockItems);
        return (int) Math.min(incoming, Math.min(Integer.MAX_VALUE, room));
    }

    public static boolean matchesTemplate(ItemStack template, ItemStack incoming) {
        if (template == null || template.isEmpty() || incoming == null || incoming.isEmpty()) {
            return false;
        }
        return ItemStack.isSameItemSameComponents(template, incoming);
    }
}
