package com.mawlee.cointcore.luckperms;

import com.mawlee.cointcore.prefix.PrefixSnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * LuckPerms inheritance permission keys are {@code group.<name>}.
 * A generic {@code Node.builder(name)} call writes a plain permission instead.
 */
public final class InheritanceNodes {
    public static final String KEY_PREFIX = "group.";

    private InheritanceNodes() {
    }

    public static String permissionKey(String group) {
        String name = normalize(group);
        return name == null ? null : KEY_PREFIX + name;
    }

    public static boolean isInheritanceKey(String key) {
        if (key == null || key.length() <= KEY_PREFIX.length()) {
            return false;
        }
        return key.regionMatches(true, 0, KEY_PREFIX, 0, KEY_PREFIX.length());
    }

    public static List<String> groupsNotInherited(Collection<String> requested, Collection<String> held) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        Set<String> have = new LinkedHashSet<>();
        if (held != null) {
            for (String name : held) {
                String normalized = normalize(name);
                if (normalized != null) {
                    have.add(normalized);
                }
            }
        }
        List<String> missing = new ArrayList<>();
        for (String name : requested) {
            String normalized = normalize(name);
            if (normalized != null && !have.contains(normalized) && !missing.contains(normalized)) {
                missing.add(normalized);
            }
        }
        return List.copyOf(missing);
    }

    private static String normalize(String group) {
        return PrefixSnapshot.normalize(group == null ? null : group.trim());
    }
}
