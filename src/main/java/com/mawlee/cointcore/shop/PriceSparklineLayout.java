package com.mawlee.cointcore.shop;

/**
 * Maps buy-price samples onto a sparkline: y=0 is the top (highest price).
 */
public final class PriceSparklineLayout {
    private PriceSparklineLayout() {
    }

    public static int[] toY(long[] values, int innerHeight) {
        int height = Math.max(1, innerHeight);
        if (values == null || values.length == 0) {
            return new int[0];
        }
        long min = values[0];
        long max = values[0];
        for (long value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        int[] ys = new int[values.length];
        if (max <= min) {
            int mid = (height - 1) / 2;
            for (int index = 0; index < values.length; index++) {
                ys[index] = mid;
            }
            return ys;
        }
        double span = max - min;
        for (int index = 0; index < values.length; index++) {
            double t = (values[index] - min) / span;
            ys[index] = (int) Math.round((1.0d - t) * (height - 1));
        }
        return ys;
    }

    public static int xForIndex(int index, int count, int innerWidth) {
        int width = Math.max(1, innerWidth);
        if (count <= 1) {
            return width / 2;
        }
        return (int) Math.round(index * (width - 1) / (double) (count - 1));
    }
}
