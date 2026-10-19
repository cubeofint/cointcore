package com.mawlee.cointcore.pipez;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Decision helper for Pipez item pipes targeting Functional Storage Armory Cabinets.
 * Certification is slot-independent in FS 1.5.8 ({@code isItemValid} → {@code isCertifiedStack}).
 */
public final class PipezArmoryFastReject {
    public static final String ARMORY_HANDLER_CLASS =
            "com.buuz135.functionalstorage.inventory.ArmoryCabinetInventoryHandler";

    private static final AtomicLong SKIPPED_INSERTS = new AtomicLong();

    private PipezArmoryFastReject() {
    }

    /**
     * Skip the dest-slot loop when the handler is an armory cabinet and the stack
     * is not certified. Empty stacks and non-armory dests always fall through.
     */
    public static boolean shouldSkipInsert(
            boolean enabled,
            boolean destIsArmoryCabinet,
            boolean stackEmpty,
            boolean certifiedForCabinet
    ) {
        return enabled && destIsArmoryCabinet && !stackEmpty && !certifiedForCabinet;
    }

    public static boolean isArmoryCabinet(Object dest) {
        return dest != null && typeIsArmoryCabinet(dest.getClass());
    }

    public static boolean typeIsArmoryCabinet(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            if (ARMORY_HANDLER_CLASS.equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    public static void recordSkippedInsert() {
        SKIPPED_INSERTS.incrementAndGet();
    }

    public static long skippedInserts() {
        return SKIPPED_INSERTS.get();
    }

    public static String statusLine(boolean enabled) {
        return String.format(
                Locale.ROOT,
                "Pipez armory fast-reject: enabled=%s skipped=%d",
                enabled,
                skippedInserts()
        );
    }

    public static void resetSkippedInserts() {
        SKIPPED_INSERTS.set(0L);
    }
}
