package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.shop.MarketSearchPolicy;
import com.mawlee.cointcore.shop.MarketSearchQuery;
import com.mawlee.cointcore.shop.MarketSearchSeller;
import com.mawlee.cointcore.shop.MarketSearchTarget;
import com.mawlee.cointcore.shop.jei.JeiSearchBridge;
import net.minecraft.world.item.ItemStack;

/**
 * Market listing filter: JEI ingredient search when the overlay is live,
 * otherwise the built-in JEI-syntax parser on the listing stack.
 */
public final class MarketSearch {
    private MarketSearch() {
    }

    public static boolean matches(String query, ItemStack stack, String modId) {
        return matches(query, stack, modId, "");
    }

    public static boolean matches(String query, ItemStack stack, String modId, String sellerName) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String itemQuery = MarketSearchSeller.strip(query);
        MarketSearchTarget itemTarget = MarketSearchTargets.from(stack, modId);
        MarketSearchTarget fullTarget = itemTarget.withSeller(sellerName);
        boolean localItem = itemQuery.isBlank() || MarketSearchQuery.matches(itemQuery, itemTarget);
        boolean localFull = MarketSearchQuery.matches(query, fullTarget);
        Boolean jei = itemQuery.isBlank() ? Boolean.TRUE : JeiSearchBridge.matchesListing(itemQuery, stack);
        boolean itemHit = itemQuery.isBlank()
                || MarketSearchPolicy.decide(localItem, jei, MarketSearchQuery.hasNegation(itemQuery));
        if (!MarketSearchSeller.hasTerms(query)) {
            return itemHit;
        }
        return itemHit && localFull;
    }
}
