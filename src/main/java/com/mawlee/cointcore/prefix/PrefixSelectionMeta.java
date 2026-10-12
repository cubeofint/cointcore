package com.mawlee.cointcore.prefix;

/**
 * User-level selection is stored only as meta {@code cointcore-prefix-select}.
 * The previous {@code cointcore-quest-prefix} key is still recognized so it can be cleared.
 */
public final class PrefixSelectionMeta {
    public static final String KEY = "cointcore-prefix-select";
    public static final String LEGACY_KEY = "cointcore-quest-prefix";

    private PrefixSelectionMeta() {
    }

    public static boolean isSelectionKey(String key, String configuredKey) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return KEY.equalsIgnoreCase(key)
                || LEGACY_KEY.equalsIgnoreCase(key)
                || (configuredKey != null && !configuredKey.isBlank() && configuredKey.equalsIgnoreCase(key));
    }

    public static boolean isCanonicalKey(String key) {
        return key != null && KEY.equalsIgnoreCase(key);
    }
}
