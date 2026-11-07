package com.mawlee.cointcore.modularbees;

import net.minecraft.world.inventory.DataSlot;

/**
 * Container data is sent as shorts, so each per-slot amount travels as two 16-bit halves. On the client both
 * halves are kept in {@code halves} and the combined amount is written into the client copy of the hatch.
 */
public final class ImportAmountDataSlot extends DataSlot {
    private final ImportAmountHost host;
    private final int slot;
    private final boolean high;
    private final int[] halves;

    private ImportAmountDataSlot(ImportAmountHost host, int slot, boolean high, int[] halves) {
        this.host = host;
        this.slot = slot;
        this.high = high;
        this.halves = halves;
    }

    /** Low then high half for every slot, in the same order on both sides. */
    public static void addAll(ImportAmountHost host, java.util.function.Consumer<DataSlot> add) {
        for (int slot = 0; slot < ImportAmountHost.SLOTS; slot++) {
            int[] halves = new int[2];
            add.accept(new ImportAmountDataSlot(host, slot, false, halves));
            add.accept(new ImportAmountDataSlot(host, slot, true, halves));
        }
    }

    @Override
    public int get() {
        return ImportAmountPacking.half(host.cointcore$amount(slot), high);
    }

    @Override
    public void set(int value) {
        halves[high ? 1 : 0] = value;
        host.cointcore$setAmount(slot, ImportAmountPacking.combine(halves[0], halves[1]));
    }
}
