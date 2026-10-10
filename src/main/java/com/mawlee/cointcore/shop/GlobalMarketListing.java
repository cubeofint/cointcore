package com.mawlee.cointcore.shop;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * One escrowed sell listing. {@link #stack()} count is items per deal (components preserved).
 */
public final class GlobalMarketListing {
    private final UUID id;
    private final UUID sellerId;
    private final String sellerName;
    private final ItemStack stack;
    private int dealsLeft;
    private final int pricePerDeal;
    private final long createdAt;
    private final long expiresAt;
    private int soldDeals;

    public GlobalMarketListing(
            UUID id,
            UUID sellerId,
            String sellerName,
            ItemStack stack,
            int dealsLeft,
            int pricePerDeal,
            long createdAt,
            long expiresAt,
            int soldDeals
    ) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.sellerId = sellerId;
        this.sellerName = sellerName == null ? "" : sellerName;
        ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
        copy.setCount(Math.max(1, copy.getCount()));
        this.stack = copy;
        this.dealsLeft = dealsLeft < 1 ? 0 : GlobalMarketMath.clampDeals(dealsLeft);
        this.pricePerDeal = GlobalMarketMath.clampPrice(pricePerDeal);
        this.createdAt = Math.max(0L, createdAt);
        this.expiresAt = Math.max(0L, expiresAt);
        this.soldDeals = Math.max(0, soldDeals);
    }

    public UUID id() {
        return id;
    }

    public UUID sellerId() {
        return sellerId;
    }

    public String sellerName() {
        return sellerName;
    }

    public ItemStack stack() {
        return stack.copy();
    }

    public int countPerDeal() {
        return Math.max(1, stack.getCount());
    }

    public int dealsLeft() {
        return dealsLeft;
    }

    public int pricePerDeal() {
        return pricePerDeal;
    }

    public long createdAt() {
        return createdAt;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public int soldDeals() {
        return soldDeals;
    }

    public boolean active(long now) {
        return sellerId != null
                && !stack.isEmpty()
                && dealsLeft >= 1
                && pricePerDeal >= 1
                && !GlobalMarketMath.expired(now, expiresAt);
    }

    public ItemStack takeDeals(int deals) {
        int take = Math.min(GlobalMarketMath.clampDeals(deals), dealsLeft);
        if (take < 1) {
            return ItemStack.EMPTY;
        }
        int items = GlobalMarketMath.itemsForDeals(countPerDeal(), take);
        dealsLeft -= take;
        soldDeals += take;
        ItemStack goods = stack.copy();
        goods.setCount(items);
        return goods;
    }

    public ItemStack remainingGoods() {
        if (dealsLeft < 1) {
            return ItemStack.EMPTY;
        }
        ItemStack goods = stack.copy();
        goods.setCount(GlobalMarketMath.itemsForDeals(countPerDeal(), dealsLeft));
        dealsLeft = 0;
        return goods;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        if (sellerId != null) {
            tag.putUUID("seller_id", sellerId);
        }
        tag.putString("seller_name", sellerName);
        tag.putInt("deals_left", dealsLeft);
        tag.putInt("price", pricePerDeal);
        tag.putLong("created_at", createdAt);
        tag.putLong("expires_at", expiresAt);
        tag.putInt("sold_deals", soldDeals);
        if (!stack.isEmpty()) {
            tag.put("item", stack.save(registries));
        }
        return tag;
    }

    public static GlobalMarketListing load(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack item = ItemStack.EMPTY;
        if (tag.contains("item")) {
            item = ItemStack.parse(registries, tag.get("item")).orElse(ItemStack.EMPTY);
        }
        UUID id = tag.hasUUID("id") ? tag.getUUID("id") : UUID.randomUUID();
        UUID seller = tag.hasUUID("seller_id") ? tag.getUUID("seller_id") : null;
        return new GlobalMarketListing(
                id,
                seller,
                tag.getString("seller_name"),
                item,
                tag.getInt("deals_left"),
                tag.getInt("price"),
                tag.getLong("created_at"),
                tag.getLong("expires_at"),
                tag.getInt("sold_deals")
        );
    }
}
