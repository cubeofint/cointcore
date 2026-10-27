package com.mawlee.cointcore.refinedstorage.wireless;

import net.minecraft.world.inventory.DataSlot;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/** Container property packets carry signed shorts; synchronize the count as four unsigned parts. */
public final class NetworkPatternCountPart extends DataSlot {
    private final LongSupplier read;
    private final LongConsumer write;
    private final int shift;

    public NetworkPatternCountPart(LongSupplier read, LongConsumer write, int shift) {
        this.read = read;
        this.write = write;
        this.shift = shift;
    }

    @Override public int get() { return (int) (read.getAsLong() >>> shift) & 0xffff; }
    @Override public void set(int value) {
        write.accept((read.getAsLong() & ~(0xffffL << shift)) | ((value & 0xffffL) << shift));
    }
}
