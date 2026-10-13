package com.mawlee.cointcore.watchdog;

/**
 * Cross-thread snapshot of the object currently being timed on the server thread.
 * Written by mixins, read by {@link StackSampler}.
 *
 * <p>Hot path stores object refs only. Registry ids are resolved when {@link #snapshot()}
 * is taken on the sampler thread.
 */
public final class TickProbe {
    public record Snapshot(
            boolean active,
            String kind,
            String typeId,
            String modId,
            String dimension,
            int x,
            int y,
            int z
    ) {
        public static Snapshot inactive() {
            return new Snapshot(false, "", "", "", "", 0, 0, 0);
        }
    }

    private static volatile boolean active;
    private static volatile String kind = "";
    private static volatile Object typeRef;
    private static volatile Object dimensionRef;
    private static volatile int x;
    private static volatile int y;
    private static volatile int z;
    private static volatile WatchdogLabels labels = WatchdogLabels.STRINGS;

    private TickProbe() {
    }

    public static void setLabels(WatchdogLabels value) {
        labels = value == null ? WatchdogLabels.STRINGS : value;
    }

    public static void enterBlockEntity(
            String typeIdValue,
            String modIdValue,
            String dimensionValue,
            int posX,
            int posY,
            int posZ
    ) {
        enter("block_entity", typeIdValue, dimensionValue, posX, posY, posZ);
    }

    public static void enterEntity(
            String typeIdValue,
            String modIdValue,
            String dimensionValue,
            int posX,
            int posY,
            int posZ
    ) {
        enter("entity", typeIdValue, dimensionValue, posX, posY, posZ);
    }

    public static void enter(String kindValue, Object type, Object dimension, int posX, int posY, int posZ) {
        kind = kindValue == null ? "" : kindValue;
        typeRef = type;
        dimensionRef = dimension;
        x = posX;
        y = posY;
        z = posZ;
        active = true;
    }

    public static void exit() {
        active = false;
    }

    public static Snapshot snapshot() {
        if (!active) {
            return Snapshot.inactive();
        }
        WatchdogLabels resolver = labels;
        String typeId = resolver.typeId(typeRef);
        String dimension = resolver.dimensionId(dimensionRef);
        return new Snapshot(
                true,
                kind,
                typeId,
                WatchdogLabels.owningMod(typeId),
                dimension,
                x,
                y,
                z
        );
    }

    public static void clear() {
        active = false;
        kind = "";
        typeRef = null;
        dimensionRef = null;
        x = 0;
        y = 0;
        z = 0;
    }
}
