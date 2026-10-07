package com.mawlee.cointcore.shop;

import com.mawlee.cointcore.CointCore;
import com.mawlee.cointcore.config.TraderOffersConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TraderPriceHistorySavedData extends SavedData {
    private static final String DATA_ID = CointCore.MOD_ID + "_trader_price_history";
    private static final String OFFERS_KEY = "offers";
    private static final String ID_KEY = "id";
    private static final String PRICES_KEY = "prices";

    private final Map<String, OfferBuyPriceHistory> byOfferId = new HashMap<>();

    private TraderPriceHistorySavedData() {
    }

    public static TraderPriceHistorySavedData get(MinecraftServer server) {
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(TraderPriceHistorySavedData::new, TraderPriceHistorySavedData::load),
                        DATA_ID
                );
    }

    public synchronized void record(String offerId, long buyPrice) {
        if (offerId == null || offerId.isBlank() || buyPrice <= 0L) {
            return;
        }
        history(offerId).record(buyPrice);
        setDirty();
    }

    public synchronized void sampleOffers(List<TraderOffer> offers) {
        if (offers == null || offers.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (TraderOffer offer : offers) {
            if (offer.buyPrice() <= 0L) {
                continue;
            }
            history(offer.id()).record(offer.buyPrice());
            changed = true;
        }
        if (changed) {
            setDirty();
        }
    }

    public synchronized long[] snapshot(String offerId) {
        OfferBuyPriceHistory history = byOfferId.get(offerId);
        if (history == null) {
            return new long[0];
        }
        return history.snapshot();
    }

    private OfferBuyPriceHistory history(String offerId) {
        return byOfferId.computeIfAbsent(
                offerId,
                ignored -> new OfferBuyPriceHistory(TraderOffersConfig.priceHistoryCapacity())
        );
    }

    private static TraderPriceHistorySavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        TraderPriceHistorySavedData data = new TraderPriceHistorySavedData();
        int capacity = TraderOffersConfig.priceHistoryCapacity();
        if (!tag.contains(OFFERS_KEY, Tag.TAG_LIST)) {
            return data;
        }
        ListTag list = tag.getList(OFFERS_KEY, Tag.TAG_COMPOUND);
        for (Tag entryTag : list) {
            CompoundTag entry = (CompoundTag) entryTag;
            String id = entry.getString(ID_KEY);
            if (id.isBlank()) {
                continue;
            }
            long[] prices = entry.contains(PRICES_KEY) ? entry.getLongArray(PRICES_KEY) : new long[0];
            data.byOfferId.put(id, OfferBuyPriceHistory.fromSnapshot(prices, capacity));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Map.Entry<String, OfferBuyPriceHistory> entry : byOfferId.entrySet()) {
            CompoundTag offerTag = new CompoundTag();
            offerTag.putString(ID_KEY, entry.getKey());
            offerTag.putLongArray(PRICES_KEY, entry.getValue().snapshot());
            list.add(offerTag);
        }
        tag.put(OFFERS_KEY, list);
        return tag;
    }
}
