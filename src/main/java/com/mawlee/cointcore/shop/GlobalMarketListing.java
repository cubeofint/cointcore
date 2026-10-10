package com.mawlee.cointcore.shop;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Escrowed sell listing: template + items per deal + price, backed by a long stock.
 */
public final class GlobalMarketListing {
    private final UUID id;
    private final UUID sellerId;
    private final String sellerName;
    private final ItemStack template;
    private int countPerDeal;
    private int pricePerDeal;
    private long stockItems;
    private final long createdAt;
    private final long expiresAt;
    private long soldDeals;

    public GlobalMarketListing(
            UUID id,
            UUID sellerId,
            String sellerName,
            ItemStack template,
            int countPerDeal,
            int pricePerDeal,
            long stockItems,
            long createdAt,
            long expiresAt,
            long soldDeals
    ) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.sellerId = sellerId;
        this.sellerName = sellerName == null ? "" : sellerName;
        this.template = template == null || template.isEmpty() ? ItemStack.EMPTY : template.copyWithCount(1);
        this.countPerDeal = Math.max(1, PlayerShopValidation.clampCount(countPerDeal));
        this.pricePerDeal = GlobalMarketMath.clampPrice(pricePerDeal);
        this.stockItems = Math.max(0L, stockItems);
        this.createdAt = Math.max(0L, createdAt);
        this.expiresAt = Math.max(0L, expiresAt);
        this.soldDeals = Math.max(0L, soldDeals);
    }

    public static GlobalMarketListing fromLegacyDeals(
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
        ItemStack source = stack == null ? ItemStack.EMPTY : stack;
        int count = Math.max(1, source.getCount());
        return new GlobalMarketListing(
                id,
                sellerId,
                sellerName,
                source,
                count,
                pricePerDeal,
                GlobalMarketMath.itemsForDeals(count, Math.max(0, dealsLeft)),
                createdAt,
                expiresAt,
                soldDeals
        );
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
        ItemStack copy = template.copy();
        copy.setCount(countPerDeal);
        return copy;
    }

    public ItemStack template() {
        return template.copy();
    }

    public int countPerDeal() {
        return countPerDeal;
    }

    public long dealsLeft() {
        return GlobalMarketMath.dealsFromStock(stockItems, countPerDeal);
    }

    public long stockItems() {
        return stockItems;
    }

    public int pricePerDeal() {
        return pricePerDeal;
    }

    public long unitPrice() {
        return GlobalMarketPriceMath.unitPrice(pricePerDeal, countPerDeal);
    }

    public long createdAt() {
        return createdAt;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public long soldDeals() {
        return soldDeals;
    }

    public boolean active(long now) {
        return sellerId != null
                && !template.isEmpty()
                && dealsLeft() >= 1L
                && pricePerDeal >= 1
                && !GlobalMarketMath.expired(now, expiresAt);
    }

    public long takeDeals(long deals) {
        long take = Math.min(GlobalMarketMath.clampDeals(deals), dealsLeft());
        if (take < 1L) {
            return 0L;
        }
        long items = GlobalMarketMath.itemsForDeals(countPerDeal, take);
        stockItems -= items;
        soldDeals += take;
        return items;
    }

    public void restoreTaken(long deals, long items) {
        stockItems = GlobalMarketMath.saturatingAdd(stockItems, Math.max(0L, items));
        soldDeals = Math.max(0L, soldDeals - Math.max(0L, deals));
    }

    public long remainingStock() {
        long leftover = stockItems;
        stockItems = 0L;
        return leftover;
    }

    public long addStock(long items) {
        long add = Math.max(0L, items);
        stockItems = GlobalMarketMath.saturatingAdd(stockItems, add);
        return stockItems;
    }

    public void applyEdit(int newCount, int newPrice, long newStock) {
        this.countPerDeal = Math.max(1, PlayerShopValidation.clampCount(newCount));
        this.pricePerDeal = GlobalMarketMath.clampPrice(newPrice);
        this.stockItems = Math.max(0L, newStock);
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        if (sellerId != null) {
            tag.putUUID("seller_id", sellerId);
        }
        tag.putString("seller_name", sellerName);
        tag.putInt("count", countPerDeal);
        tag.putLong("stock", stockItems);
        tag.putInt("price", pricePerDeal);
        tag.putLong("created_at", createdAt);
        tag.putLong("expires_at", expiresAt);
        tag.putLong("sold_deals", soldDeals);
        if (!template.isEmpty()) {
            tag.put("item", template.save(registries));
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
        int count = tag.contains("count") ? tag.getInt("count") : Math.max(1, item.getCount());
        long stock;
        if (tag.contains("stock")) {
            stock = tag.getLong("stock");
        } else {
            stock = GlobalMarketMath.itemsForDeals(count, Math.max(0, tag.getInt("deals_left")));
        }
        long sold = tag.contains("sold_deals") ? tag.getLong("sold_deals") : tag.getInt("sold_deals");
        return new GlobalMarketListing(
                id,
                seller,
                tag.getString("seller_name"),
                item,
                count,
                tag.getInt("price"),
                stock,
                tag.getLong("created_at"),
                tag.getLong("expires_at"),
                sold
        );
    }
}
