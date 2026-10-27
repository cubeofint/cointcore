package com.mawlee.cointcore.refinedstorage.wireless;

import net.minecraft.nbt.CompoundTag;

/** Keeps the editor snapshot isolated from other components and from the live mutable editor. */
public final class WirelessPatternState {
    public static final String KEY = "cointcore_wireless_patterns";

    private WirelessPatternState() {
    }

    public static CompoundTag read(CompoundTag customData) {
        return customData.getCompound(KEY).copy();
    }

    public static void write(CompoundTag customData, CompoundTag editor) {
        customData.put(KEY, editor.copy());
    }
}
