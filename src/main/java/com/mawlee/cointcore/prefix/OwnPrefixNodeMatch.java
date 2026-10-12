package com.mawlee.cointcore.prefix;

import java.util.Collection;

/**
 * Identifies cointcore-managed user PREFIX nodes without NodeMetadata
 * (LuckPerms does not persist metadata, so it cannot be used after restart).
 *
 * <p>A user PREFIX node is ours when its priority equals the configured
 * {@code prefix_priority} (default 5, below staff/donor 70–110 so
 * {@code highest_not_on_track_ranks} wins slot 1), or the legacy write
 * priority 1000, or its text equals any ranks-track group prefix
 * regardless of priority.
 */
public final class OwnPrefixNodeMatch {
    public static final int LEGACY_PREFIX_PRIORITY = 1000;

    private OwnPrefixNodeMatch() {
    }

    public static boolean matches(
            int priority,
            String prefixText,
            int configuredPriority,
            Collection<String> rankPrefixTexts
    ) {
        if (priority == configuredPriority || priority == LEGACY_PREFIX_PRIORITY) {
            return true;
        }
        if (prefixText == null || prefixText.isBlank() || rankPrefixTexts == null || rankPrefixTexts.isEmpty()) {
            return false;
        }
        return rankPrefixTexts.contains(prefixText);
    }
}
