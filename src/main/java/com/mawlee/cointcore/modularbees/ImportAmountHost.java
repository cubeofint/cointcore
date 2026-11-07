package com.mawlee.cointcore.modularbees;

import com.mawlee.cointcore.config.ModularBeesRsConfig;
import net.minecraft.world.item.ItemStack;

/** Per-slot amounts of a ModularBees ME import hatch, set by players with a middle click on a config slot. */
public interface ImportAmountHost {
    int SLOTS = 9;
    String SET_ACTION = "cointcore_import_amount";

    /** Player-chosen amount for the slot, or 0 for the config default. */
    int cointcore$amount(int slot);

    void cointcore$setAmount(int slot, int amount);

    /** How many of {@code stack} the slot should hold. */
    default int cointcore$target(int slot, ItemStack stack) {
        int custom = slot >= 0 && slot < SLOTS ? cointcore$amount(slot) : 0;
        if (custom > 0) return custom;
        long fallback = (long) Math.max(1, stack.getMaxStackSize()) * ModularBeesRsConfig.getImportBufferStacks();
        return (int) Math.min(Integer.MAX_VALUE, fallback);
    }
}
