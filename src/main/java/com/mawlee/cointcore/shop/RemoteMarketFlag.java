package com.mawlee.cointcore.shop;

/** {@code remote_market_enabled} in trader_offers.json. Null means on. */
public final class RemoteMarketFlag {
    private RemoteMarketFlag() {
    }

    public static boolean enabled(Boolean configured) {
        return configured == null || configured;
    }
}
