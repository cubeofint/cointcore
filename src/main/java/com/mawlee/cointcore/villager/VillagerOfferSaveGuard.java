package com.mawlee.cointcore.villager;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/** Excludes invalid trades from a save snapshot, retaining live offers and every valid trade unchanged. */
public final class VillagerOfferSaveGuard {
    private VillagerOfferSaveGuard() { }

    public static MerchantOffers forSave(MerchantOffers offers) {
        MerchantOffers filtered = null;
        for (int i = 0; i < offers.size(); i++) {
            var offer = offers.get(i);
            if (!isSaveable(offer)) {
                if (filtered == null) {
                    filtered = new MerchantOffers();
                    filtered.addAll(offers.subList(0, i));
                }
            } else if (filtered != null) {
                filtered.add(offer);
            }
        }
        return filtered == null ? offers : filtered;
    }

    private static boolean isSaveable(MerchantOffer offer) {
        if (offer == null || !validCost(offer.getItemCostA())) return false;
        if (offer.getItemCostB().isPresent() && !validCost(offer.getItemCostB().get())) return false;
        var result = offer.getResult();
        // ItemStack.CODEC requires a non-air item and a count in [1, 99]. Input costs use POSITIVE_INT.
        return result != null && !result.isEmpty() && result.getCount() <= 99;
    }

    private static boolean validCost(ItemCost cost) {
        return cost != null && cost.count() > 0 && cost.item().value() != Items.AIR;
    }
}
