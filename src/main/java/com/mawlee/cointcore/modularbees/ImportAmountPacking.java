package com.mawlee.cointcore.modularbees;

/** Container data travels as shorts, so an int amount is sent as two 16-bit halves. */
public final class ImportAmountPacking {
    private ImportAmountPacking() {
    }

    /** The 16 bits of {@code amount} one data slot carries. */
    public static int half(int amount, boolean high) {
        return (high ? amount >>> 16 : amount) & 0xFFFF;
    }

    /** Rebuilds the amount from the two received values (which arrive sign-extended from a short). */
    public static int combine(int low, int high) {
        return (high & 0xFFFF) << 16 | low & 0xFFFF;
    }
}
