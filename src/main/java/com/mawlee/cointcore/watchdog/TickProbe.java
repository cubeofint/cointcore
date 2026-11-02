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

    private static final PublishedWatchdogContext CONTEXT = new PublishedWatchdogContext();
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
        CONTEXT.enter(1, kindValue == null ? "" : kindValue, 0, type, dimension,
                posX, posY, posZ, "", "");
    }

    public static void exit() {
        CONTEXT.exit();
    }

    public static Snapshot snapshot() {
        var context = CONTEXT.snapshot();
        if (context.kind() == 0) {
            return Snapshot.inactive();
        }
        WatchdogLabels resolver = labels;
        String typeId = resolver.typeId(context.type());
        String dimension = resolver.dimensionId(context.dimension());
        return new Snapshot(
                true,
                context.label(),
                typeId,
                WatchdogLabels.owningMod(typeId),
                dimension,
                context.x(),
                context.y(),
                context.z()
        );
    }

    public static void clear() {
        CONTEXT.clear();
    }
}
