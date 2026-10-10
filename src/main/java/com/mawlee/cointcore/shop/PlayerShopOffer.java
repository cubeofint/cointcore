package com.mawlee.cointcore.shop;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Owner-defined listing for a player vending machine. {@code buyPrice} is the
 * sell-to-customer price; {@code sellPrice} is the buy-from-customer price.
 * Zero disables that direction. Template count is ignored; {@link #count()} is the lot size.
 */
public final class PlayerShopOffer {
    private final String id;
    private UUID sellerId;
    private String sellerName;
    private ItemStack template;
    private int count;
    private long buyPrice;
    private long sellPrice;

    public PlayerShopOffer(String id, ItemStack template, int count, long buyPrice, long sellPrice) {
        this(id, null, "", template, count, buyPrice, sellPrice);
    }

    public PlayerShopOffer(
            String id,
            UUID sellerId,
            String sellerName,
            ItemStack template,
            int count,
            long buyPrice,
            long sellPrice
    ) {
        this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        this.sellerId = sellerId;
        this.sellerName = sellerName == null ? "" : sellerName;
        this.template = template == null ? ItemStack.EMPTY : template.copy();
        this.template.setCount(1);
        this.count = PlayerShopValidation.clampCount(count);
        this.buyPrice = PlayerShopValidation.clampPrice(buyPrice);
        this.sellPrice = PlayerShopValidation.clampPrice(sellPrice);
    }

    public String id() {
        return id;
    }

    public UUID sellerId() {
        return sellerId;
    }

    public String sellerName() {
        return sellerName == null ? "" : sellerName;
    }

    public void setSeller(UUID sellerId, String sellerName) {
        this.sellerId = sellerId;
        this.sellerName = sellerName == null ? "" : sellerName;
    }

    public ItemStack template() {
        return template.copy();
    }

    public ItemStack sample() {
        ItemStack stack = template.copy();
        stack.setCount(Math.max(1, count));
        return stack;
    }

    public int count() {
        return count;
    }

    public long buyPrice() {
        return buyPrice;
    }

    public long sellPrice() {
        return sellPrice;
    }

    public void setTemplate(ItemStack stack) {
        template = stack == null ? ItemStack.EMPTY : stack.copy();
        template.setCount(1);
    }

    public void setCount(int count) {
        this.count = PlayerShopValidation.clampCount(count);
    }

    public void setBuyPrice(long buyPrice) {
        this.buyPrice = PlayerShopValidation.clampPrice(buyPrice);
    }

    public void setSellPrice(long sellPrice) {
        this.sellPrice = PlayerShopValidation.clampPrice(sellPrice);
    }

    public boolean isValid() {
        return PlayerShopValidation.offerValid(template.isEmpty(), count, buyPrice, sellPrice);
    }

    public TraderOffer toTraderOffer(double commissionPercent) {
        return TraderOffer.of(id, sample(), count, buyPrice, sellPrice, commissionPercent);
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        if (sellerId != null) {
            tag.putUUID("seller_id", sellerId);
        }
        if (sellerName != null && !sellerName.isBlank()) {
            tag.putString("seller_name", sellerName);
        }
        tag.putInt("count", count);
        tag.putLong("buy_price", buyPrice);
        tag.putLong("sell_price", sellPrice);
        if (!template.isEmpty()) {
            tag.put("item", template.save(registries));
        }
        return tag;
    }

    public static PlayerShopOffer load(CompoundTag tag, HolderLookup.Provider registries) {
        String id = tag.getString("id");
        ItemStack item = ItemStack.EMPTY;
        if (tag.contains("item")) {
            item = ItemStack.parse(registries, tag.get("item")).orElse(ItemStack.EMPTY);
        }
        UUID sellerId = tag.hasUUID("seller_id") ? tag.getUUID("seller_id") : null;
        String sellerName = tag.getString("seller_name");
        return new PlayerShopOffer(
                id,
                sellerId,
                sellerName,
                item,
                tag.getInt("count"),
                tag.getLong("buy_price"),
                tag.getLong("sell_price")
        );
    }
}
