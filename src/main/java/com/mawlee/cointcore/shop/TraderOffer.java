package com.mawlee.cointcore.shop;

import net.minecraft.world.item.ItemStack;

/**
 * One trader listing. Missing/non-positive prices disable that direction.
 * Commission is paid by the charged side: buyers pay {@link #buyTotal()}, sellers receive {@link #sellNet()}.
 * {@link #buyHistory()} is chronological buy-price samples for the sparkline (oldest first).
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
        long sellNet,
        long[] buyHistory
) {
    public TraderOffer {
        buyHistory = buyHistory == null ? new long[0] : buyHistory.clone();
    }

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

    public TraderOffer withBuyHistory(long[] history) {
        return new TraderOffer(id, display, count, buyPrice, sellPrice, buyFee, buyTotal, sellFee, sellNet, history);
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
        return new TraderOffer(id, stack, safeCount, safeBuy, safeSell, buy.fee(), buy.total(), sell.fee(), net, new long[0]);
    }
}
