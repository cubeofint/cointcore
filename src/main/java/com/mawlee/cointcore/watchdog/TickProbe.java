package com.mawlee.cointcore.watchdog;

/**
 * Cross-thread snapshot of the object currently being timed on the server thread.
 * Written by mixins, read by {@link StackSampler}.
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
    private static volatile String typeId = "";
    private static volatile String modId = "";
    private static volatile String dimension = "";
    private static volatile int x;
    private static volatile int y;
    private static volatile int z;

    private TickProbe() {
    }

    public static void enterBlockEntity(String typeIdValue, String modIdValue, String dimensionValue, int posX, int posY, int posZ) {
        kind = "block_entity";
        typeId = typeIdValue == null ? "" : typeIdValue;
        modId = modIdValue == null ? "" : modIdValue;
        dimension = dimensionValue == null ? "" : dimensionValue;
        x = posX;
        y = posY;
        z = posZ;
        active = true;
    }

    public static void enterEntity(String typeIdValue, String modIdValue, String dimensionValue, int posX, int posY, int posZ) {
        kind = "entity";
        typeId = typeIdValue == null ? "" : typeIdValue;
        modId = modIdValue == null ? "" : modIdValue;
        dimension = dimensionValue == null ? "" : dimensionValue;
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
        return new Snapshot(true, kind, typeId, modId, dimension, x, y, z);
    }

    public static void clear() {
        active = false;
        kind = "";
        typeId = "";
        modId = "";
        dimension = "";
        x = 0;
        y = 0;
        z = 0;
    }
}
