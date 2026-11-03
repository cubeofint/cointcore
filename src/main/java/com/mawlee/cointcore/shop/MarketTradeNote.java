package com.mawlee.cointcore.shop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Item details are recorded at trade time, so removing a listing does not erase its history. */
public final class MarketTradeNote {
    private MarketTradeNote() { }

    public static String of(String offer, ItemStack item, long count, long fee) {
        return offer + " | " + count + "x " + BuiltInRegistries.ITEM.getKey(item.getItem())
                + " (" + item.getHoverName().getString().replace('\n', ' ').replace('\r', ' ')
                + ") | fee=" + fee;
    }
}
