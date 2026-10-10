package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.config.TraderOffersConfig;
import net.minecraft.world.item.ItemStack;

/**
 * Phantom listing sample: a count-1 copy with components, never a real item.
 */
public final class GhostTemplate {
    private GhostTemplate() {
    }

    public static ItemStack sanitize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack unit = stack.copyWithCount(1);
        if (GlobalMarketBlacklist.forbidden(unit, TraderOffersConfig.playerShopItemBlacklist())) {
            return ItemStack.EMPTY;
        }
        return unit;
    }

    public static boolean isClearClick(int button, ItemStack carried) {
        return button == 1 || carried == null || carried.isEmpty();
    }
}
