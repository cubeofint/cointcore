package com.mawlee.cointcore.shop.client;

import com.mawlee.cointcore.shop.MarketSearchPolicy;
import com.mawlee.cointcore.shop.MarketSearchQuery;
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
        if (query == null || query.isBlank()) {
            return true;
        }
        MarketSearchTarget target = MarketSearchTargets.from(stack, modId);
        boolean local = MarketSearchQuery.matches(query, target);
        Boolean jei = JeiSearchBridge.matchesListing(query, stack);
        return MarketSearchPolicy.decide(local, jei, MarketSearchQuery.hasNegation(query));
    }
}
