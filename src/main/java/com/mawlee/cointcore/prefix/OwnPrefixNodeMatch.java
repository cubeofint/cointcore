package com.mawlee.cointcore.prefix;

import java.util.Collection;

/**
 * Identifies cointcore-managed user PREFIX nodes without NodeMetadata
 * (LuckPerms does not persist metadata, so it cannot be used after restart).
 *
 * <p>A user PREFIX node is ours when its priority equals {@code prefix_priority}
 * or its text equals any ranks-track group prefix.
 */
public final class OwnPrefixNodeMatch {
    private OwnPrefixNodeMatch() {
    }

    public static boolean matches(
            int priority,
            String prefixText,
            int configuredPriority,
            Collection<String> rankPrefixTexts
    ) {
        if (priority == configuredPriority) {
            return true;
        }
        if (prefixText == null || prefixText.isBlank() || rankPrefixTexts == null || rankPrefixTexts.isEmpty()) {
            return false;
        }
        return rankPrefixTexts.contains(prefixText);
    }
}
