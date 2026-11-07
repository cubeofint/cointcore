package com.mawlee.cointcore.modularbees;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportAmountPackingTest {
    /** Vanilla sends container data as a short, so the client sees sign-extended 16-bit values. */
    private static int overTheWire(int value) {
        return (short) value;
    }

    @Test void amountsUpToIntMaxSurviveTheShortSizedContainerData() {
        for (int amount : new int[]{0, 1, 4096, 32767, 32768, 65535, 65536, 70000, 1_000_000_000, Integer.MAX_VALUE}) {
            int low = overTheWire(ImportAmountPacking.half(amount, false));
            int high = overTheWire(ImportAmountPacking.half(amount, true));
            assertEquals(amount, ImportAmountPacking.combine(low, high), "amount " + amount);
        }
    }
}
