package com.mawlee.cointcore.refinedstorage.wireless;

import java.util.function.IntPredicate;

/** Find a hotbar destination while preserving an occupied selected slot. */
public record PickSlotPlan(int target, int relocate) {
    public static PickSlotPlan choose(int selected, IntPredicate empty, IntPredicate accepts) {
        if (accepts.test(selected)) return new PickSlotPlan(selected, -1);
        for (int slot = 0; slot < 9; slot++) {
            if (accepts.test(slot)) return new PickSlotPlan(slot, -1);
        }
        for (int slot = 9; slot < 36; slot++) {
            if (empty.test(slot)) return new PickSlotPlan(selected, slot);
        }
        return new PickSlotPlan(-1, -1);
    }
}
