package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.config.TraderOffersConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GlobalMarketSavedData extends SavedData {
    static final String DATA_ID = CointCore.MOD_ID + "_global_market";

    private final Map<UUID, GlobalMarketListing> listings = new HashMap<>();
    private final Map<UUID, List<ItemStack>> returnBoxes = new HashMap<>();
    private final Map<UUID, SoldStats> sold = new HashMap<>();
    private final Map<String, List<GlobalMarketPriceMath.Sale>> sales = new HashMap<>();

    private GlobalMarketSavedData() {
    }

    public static GlobalMarketSavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(GlobalMarketSavedData::new, GlobalMarketSavedData::load), DATA_ID);
    }

    public synchronized GlobalMarketListing listing(UUID id) {
        return id == null ? null : listings.get(id);
    }

    public synchronized List<GlobalMarketListing> allListings() {
        return List.copyOf(listings.values());
    }

    public synchronized int listingCount(UUID sellerId) {
        int count = 0;
        for (GlobalMarketListing listing : listings.values()) {
            if (sellerId.equals(listing.sellerId()) && listing.dealsLeft() > 0) {
                count++;
            }
        }
        return count;
    }

    public synchronized void put(GlobalMarketListing listing) {
        listings.put(listing.id(), listing);
        setDirty();
    }

    public synchronized GlobalMarketListing remove(UUID id) {
        GlobalMarketListing listing = listings.remove(id);
        if (listing != null) {
            setDirty();
        }
        return listing;
    }

    public synchronized void addReturn(UUID playerId, ItemStack stack) {
        if (playerId == null || stack == null || stack.isEmpty()) {
            return;
        }
        returnBoxes.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(stack.copy());
        setDirty();
    }

    public synchronized List<ItemStack> claimReturns(UUID playerId) {
        List<ItemStack> box = returnBoxes.remove(playerId);
        if (box == null || box.isEmpty()) {
            return List.of();
        }
        setDirty();
        return box;
    }

    public synchronized int returnCount(UUID playerId) {
        List<ItemStack> box = returnBoxes.get(playerId);
        return box == null ? 0 : box.size();
    }

    public synchronized void recordSold(UUID sellerId, int deals, long gluons) {
        if (sellerId == null || deals <= 0 || gluons <= 0L) {
            return;
        }
        SoldStats stats = sold.computeIfAbsent(sellerId, ignored -> new SoldStats());
        stats.deals = saturateInt(stats.deals, deals);
        stats.gluons = GlobalMarketMath.saturatingAdd(stats.gluons, gluons);
        setDirty();
    }

    public synchronized SoldStats stats(UUID sellerId) {
        SoldStats stats = sold.get(sellerId);
        return stats == null ? new SoldStats() : new SoldStats(stats.deals, stats.gluons);
    }

    public synchronized void recordSale(String itemKey, long unitPrice, long timestamp) {
        if (itemKey == null || itemKey.isBlank() || unitPrice <= 0L) {
            return;
        }
        List<GlobalMarketPriceMath.Sale> bucket = sales.computeIfAbsent(itemKey, ignored -> new ArrayList<>());
        bucket.add(new GlobalMarketPriceMath.Sale(itemKey, unitPrice, timestamp));
        List<GlobalMarketPriceMath.Sale> pruned = GlobalMarketPriceMath.prune(
                bucket,
                timestamp,
                TraderOffersConfig.marketPriceWindowDays(),
                TraderOffersConfig.marketPriceMaxSales()
        );
        bucket.clear();
        bucket.addAll(pruned);
        if (bucket.isEmpty()) {
            sales.remove(itemKey);
        }
        setDirty();
    }

    public synchronized List<Long> recentUnitPrices(String itemKey, long now) {
        if (itemKey == null || itemKey.isBlank()) {
            return List.of();
        }
        List<GlobalMarketPriceMath.Sale> bucket = sales.get(itemKey);
        if (bucket == null || bucket.isEmpty()) {
            return List.of();
        }
        List<GlobalMarketPriceMath.Sale> pruned = GlobalMarketPriceMath.prune(
                bucket,
                now,
                TraderOffersConfig.marketPriceWindowDays(),
                TraderOffersConfig.marketPriceMaxSales()
        );
        if (pruned.size() != bucket.size()) {
            bucket.clear();
            bucket.addAll(pruned);
            if (bucket.isEmpty()) {
                sales.remove(itemKey);
            }
            setDirty();
        }
        return GlobalMarketPriceMath.unitPrices(pruned);
    }

    private static int saturateInt(int current, int add) {
        if (add <= 0) {
            return current;
        }
        if (current > Integer.MAX_VALUE - add) {
            return Integer.MAX_VALUE;
        }
        return current + add;
    }

    private static GlobalMarketSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        GlobalMarketSavedData data = new GlobalMarketSavedData();
        ListTag listingTags = tag.getList("listings", Tag.TAG_COMPOUND);
        for (int index = 0; index < listingTags.size(); index++) {
            GlobalMarketListing listing = GlobalMarketListing.load(listingTags.getCompound(index), registries);
            data.listings.put(listing.id(), listing);
        }
        ListTag boxes = tag.getList("return_boxes", Tag.TAG_COMPOUND);
        for (int index = 0; index < boxes.size(); index++) {
            CompoundTag box = boxes.getCompound(index);
            if (!box.hasUUID("player_id")) {
                continue;
            }
            UUID playerId = box.getUUID("player_id");
            ListTag items = box.getList("items", Tag.TAG_COMPOUND);
            List<ItemStack> stacks = new ArrayList<>();
            for (int item = 0; item < items.size(); item++) {
                ItemStack.parse(registries, items.getCompound(item)).ifPresent(stacks::add);
            }
            if (!stacks.isEmpty()) {
                data.returnBoxes.put(playerId, stacks);
            }
        }
        ListTag soldTags = tag.getList("sold", Tag.TAG_COMPOUND);
        for (int index = 0; index < soldTags.size(); index++) {
            CompoundTag row = soldTags.getCompound(index);
            if (!row.hasUUID("player_id")) {
                continue;
            }
            data.sold.put(row.getUUID("player_id"), new SoldStats(row.getInt("deals"), row.getLong("gluons")));
        }
        ListTag saleTags = tag.getList("sales", Tag.TAG_COMPOUND);
        for (int index = 0; index < saleTags.size(); index++) {
            CompoundTag row = saleTags.getCompound(index);
            String key = row.getString("item_key");
            if (key.isBlank()) {
                continue;
            }
            data.sales.computeIfAbsent(key, ignored -> new ArrayList<>()).add(
                    new GlobalMarketPriceMath.Sale(key, row.getLong("unit_price"), row.getLong("at"))
            );
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag listingTags = new ListTag();
        for (GlobalMarketListing listing : listings.values()) {
            listingTags.add(listing.save(registries));
        }
        tag.put("listings", listingTags);
        ListTag boxes = new ListTag();
        for (Map.Entry<UUID, List<ItemStack>> entry : returnBoxes.entrySet()) {
            CompoundTag box = new CompoundTag();
            box.putUUID("player_id", entry.getKey());
            ListTag items = new ListTag();
            for (ItemStack stack : entry.getValue()) {
                if (!stack.isEmpty()) {
                    items.add(stack.save(registries, new CompoundTag()));
                }
            }
            box.put("items", items);
            boxes.add(box);
        }
        tag.put("return_boxes", boxes);
        ListTag soldTags = new ListTag();
        for (Map.Entry<UUID, SoldStats> entry : sold.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("player_id", entry.getKey());
            row.putInt("deals", entry.getValue().deals);
            row.putLong("gluons", entry.getValue().gluons);
            soldTags.add(row);
        }
        tag.put("sold", soldTags);
        ListTag saleTags = new ListTag();
        for (List<GlobalMarketPriceMath.Sale> bucket : sales.values()) {
            for (GlobalMarketPriceMath.Sale sale : bucket) {
                CompoundTag row = new CompoundTag();
                row.putString("item_key", sale.itemKey());
                row.putLong("unit_price", sale.unitPrice());
                row.putLong("at", sale.timestamp());
                saleTags.add(row);
            }
        }
        tag.put("sales", saleTags);
        return tag;
    }

    public static final class SoldStats {
        int deals;
        long gluons;

        SoldStats() {
        }

        SoldStats(int deals, long gluons) {
            this.deals = Math.max(0, deals);
            this.gluons = Math.max(0L, gluons);
        }

        public int deals() {
            return deals;
        }

        public long gluons() {
            return gluons;
        }
    }
}
