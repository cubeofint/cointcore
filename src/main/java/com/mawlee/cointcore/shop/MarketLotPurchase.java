package com.mawlee.cointcore.shop;

/**
 * Server-side checks for a buyer picking one concrete lot: it must still exist,
 * the unit price must not have gone up versus what the client saw, and stock
 * must cover the requested deals.
 */
public final class MarketLotPurchase {
    private MarketLotPurchase() {
    }

    public enum Reason {
        OK,
        MISSING,
        INACTIVE,
        OWN_SHOP,
        PRICE_UP,
        NO_STOCK,
        BAD_QTY
    }

    public record Snapshot(
            boolean exists,
            boolean active,
            boolean ownShop,
            long unitPrice,
            long dealsLeft
    ) {
        public Snapshot {
            unitPrice = Math.max(0L, unitPrice);
            dealsLeft = Math.max(0L, dealsLeft);
        }

        public static Snapshot missing() {
            return new Snapshot(false, false, false, 0L, 0L);
        }
    }

    public record Decision(Reason reason) {
        public Decision {
            reason = reason == null ? Reason.MISSING : reason;
        }

        public boolean ok() {
            return reason == Reason.OK;
        }
    }

    public static Decision validate(Snapshot lot, long expectedUnitPrice, long requestedDeals) {
        if (lot == null || !lot.exists()) {
            return new Decision(Reason.MISSING);
        }
        if (!lot.active()) {
            return new Decision(Reason.INACTIVE);
        }
        if (lot.ownShop()) {
            return new Decision(Reason.OWN_SHOP);
        }
        long requested = GlobalMarketMath.clampDeals(requestedDeals);
        if (requested < 1L) {
            return new Decision(Reason.BAD_QTY);
        }
        if (expectedUnitPrice > 0L && lot.unitPrice() > expectedUnitPrice) {
            return new Decision(Reason.PRICE_UP);
        }
        if (lot.dealsLeft() < requested) {
            return new Decision(Reason.NO_STOCK);
        }
        return new Decision(Reason.OK);
    }
}
