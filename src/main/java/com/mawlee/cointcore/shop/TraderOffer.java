package com.mawlee.cointcore.shop;

import net.minecraft.world.item.ItemStack;

/**
 * One static trader listing. Missing/non-positive prices disable that direction.
 * Commission is paid by the charged side: buyers pay {@link #buyTotal()}, sellers receive {@link #sellNet()}.
 */
public record TraderOffer(
        String id,
        ItemStack display,
        int count,
        long buyPrice,
        long sellPrice,
        long buyFee,
        long buyTotal,
        long sellFee,
        long sellNet
) {
    public boolean canBuy() {
        return buyPrice > 0L && !display.isEmpty();
    }

    public boolean canSell() {
        return sellPrice > 0L && sellNet > 0L && !display.isEmpty();
    }

    public ItemStack createGoods() {
        ItemStack stack = display.copy();
        stack.setCount(Math.max(1, count));
        return stack;
    }

    public static TraderOffer of(String id, ItemStack display, int count, long buyPrice, long sellPrice, double commissionPercent) {
        int safeCount = Math.max(1, count);
        ItemStack stack = display.copy();
        stack.setCount(safeCount);
        long safeBuy = Math.max(0L, buyPrice);
        long safeSell = Math.max(0L, sellPrice);
        Commission.Result buy = Commission.of(safeBuy, commissionPercent);
        Commission.Result sell = Commission.of(safeSell, commissionPercent);
        long net = Math.max(0L, safeSell - sell.fee());
        return new TraderOffer(id, stack, safeCount, safeBuy, safeSell, buy.fee(), buy.total(), sell.fee(), net);
    }
}
