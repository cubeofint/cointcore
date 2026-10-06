package com.mawlee.cointcore.invsee;

import java.util.Locale;
import java.util.Set;

/**
 * Which playerdata keys InvSee is allowed to write back for an offline player.
 * Dimension, position and everything else stay on the original file.
 */
public final class InvSeeNbtOverlay {
    static final Set<String> OVERLAY_KEYS = Set.of(
            "Inventory",
            "EnderItems",
            "SelectedItem",
            "SelectedItemSlot",
            "ForgeCaps",
            "neoforge:attachments"
    );

    static final Set<String> NEVER_COPY = Set.of(
            "Dimension",
            "Pos",
            "Motion",
            "Rotation",
            "PortalCooldown",
            "FallDistance",
            "Fire",
            "Air",
            "OnGround",
            "UUID",
            "UUIDLeast",
            "UUIDMost"
    );

    private InvSeeNbtOverlay() {
    }

    public static boolean shouldOverlay(String key) {
        if (key == null || key.isBlank() || NEVER_COPY.contains(key)) {
            return false;
        }
        if (OVERLAY_KEYS.contains(key)) {
            return true;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        return lower.contains("curios")
                || lower.startsWith("cosmetic")
                || lower.contains("backpack")
                || lower.contains("pocketstorage");
    }

    public static boolean mustNeverCopy(String key) {
        return key != null && NEVER_COPY.contains(key);
    }
}
