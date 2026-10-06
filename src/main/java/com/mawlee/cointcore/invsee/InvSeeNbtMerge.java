package com.mawlee.cointcore.invsee;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

final class InvSeeNbtMerge {
    private InvSeeNbtMerge() {
    }

    static CompoundTag overlayInventory(CompoundTag original, CompoundTag fakeSave) {
        CompoundTag result = original.copy();
        for (String key : fakeSave.getAllKeys()) {
            if (!InvSeeNbtOverlay.shouldOverlay(key) || InvSeeNbtOverlay.mustNeverCopy(key)) {
                continue;
            }
            Tag value = fakeSave.get(key);
            if (value != null) {
                result.put(key, value.copy());
            }
        }
        return result;
    }
}
