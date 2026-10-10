package com.mawlee.cointcore.shop;

/**
 * Live buy quote: deals → units and debit. Max is stock and wallet.
 */
public final class MarketBuyQuote {
    private MarketBuyQuote() {
    }

    public static long units(long deals, int countPerDeal) {
        return GlobalMarketMath.itemsForDeals(countPerDeal, deals);
    }

    public static long debit(long deals, long pricePerDeal, double commissionPercent) {
        if (deals < 1L || pricePerDeal < 1L) {
            return 0L;
        }
        Commission.Result unit = Commission.of(pricePerDeal, commissionPercent);
        return GlobalMarketMath.buyerDebit(pricePerDeal, unit.fee(), deals);
    }

    public static long maxDeals(long stockItems, int countPerDeal, long pricePerDeal, long balance, double commissionPercent) {
        long byStock = GlobalMarketMath.dealsFromStock(stockItems, countPerDeal);
        Commission.Result unit = Commission.of(pricePerDeal, commissionPercent);
        long byMoney = GlobalMarketMath.maxAffordableDeals(unit.total(), balance, byStock);
        return Math.min(byStock, byMoney);
    }

    public static boolean canFill(long requested, long availableAtCap, long debit, long balance) {
        return requested >= 1L && requested <= availableAtCap && debit > 0L && debit <= balance;
    }
}
