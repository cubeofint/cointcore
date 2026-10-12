package com.mawlee.cointcore.luckperms;

import com.mawlee.cointcore.prefix.PrefixSnapshot;

/**
 * LuckPerms inheritance permission keys are {@code group.<name>}.
 * A generic {@code Node.builder(name)} call writes a plain permission instead.
 */
public final class InheritanceNodes {
    public static final String KEY_PREFIX = "group.";

    private InheritanceNodes() {
    }

    public static String permissionKey(String group) {
        String name = PrefixSnapshot.normalize(group);
        return name == null ? null : KEY_PREFIX + name;
    }

    public static boolean isInheritanceKey(String key) {
        if (key == null || key.length() <= KEY_PREFIX.length()) {
            return false;
        }
        return key.regionMatches(true, 0, KEY_PREFIX, 0, KEY_PREFIX.length());
    }
}
