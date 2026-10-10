package com.mawlee.cointcore.shop;

/**
 * JEI vs local parser combination. {@code jei == null} means the runtime is absent.
 */
public final class MarketSearchPolicy {
    private MarketSearchPolicy() {
    }

    public static boolean decide(boolean local, Boolean jei, boolean negation) {
        if (jei == null) {
            return local;
        }
        if (negation) {
            return jei && local;
        }
        return jei;
    }
}
