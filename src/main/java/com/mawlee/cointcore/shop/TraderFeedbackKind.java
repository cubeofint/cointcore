package com.mawlee.cointcore.shop;

public enum TraderFeedbackKind {
    OFFER_UNAVAILABLE,
    INVENTORY_FULL,
    NOT_ENOUGH_GLUONS,
    NOT_ENOUGH_ITEMS,
    BOUGHT,
    SOLD,
    OUT_OF_STOCK,
    STOCK_FULL,
    OWNER_BROKE,
    OWN_SHOP;

    public boolean error() {
        return switch (this) {
            case OFFER_UNAVAILABLE, INVENTORY_FULL, NOT_ENOUGH_GLUONS, NOT_ENOUGH_ITEMS,
                    OUT_OF_STOCK, STOCK_FULL, OWNER_BROKE, OWN_SHOP -> true;
            case BOUGHT, SOLD -> false;
        };
    }

    public static TraderFeedbackKind fromOrdinal(int ordinal) {
        TraderFeedbackKind[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return OFFER_UNAVAILABLE;
        }
        return values[ordinal];
    }
}
