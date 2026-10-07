package com.mawlee.cointcore.shop;

public record OfferPriceStats(long min, long max, long average, long current, int count) {
    public static OfferPriceStats of(long[] points, long currentBuy) {
        if (points == null || points.length == 0) {
            return new OfferPriceStats(0L, 0L, 0L, Math.max(0L, currentBuy), 0);
        }
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long sum = 0L;
        for (long point : points) {
            min = Math.min(min, point);
            max = Math.max(max, point);
            sum += point;
        }
        long average = sum / points.length;
        long current = currentBuy > 0L ? currentBuy : points[points.length - 1];
        return new OfferPriceStats(min, max, average, current, points.length);
    }
}
