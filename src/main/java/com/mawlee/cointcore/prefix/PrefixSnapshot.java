package com.mawlee.cointcore.prefix;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Player prefix state used by {@link PrefixSelectionRules}. Independent of LuckPerms / Minecraft.
 */
public record PrefixSnapshot(
        boolean lockedStaff,
        List<PrefixOption> available,
        String selected
) {
    public PrefixSnapshot {
        available = available == null ? List.of() : List.copyOf(available);
        selected = normalize(selected);
    }

    public Set<String> availableGroups() {
        return available.stream().map(PrefixOption::group).collect(Collectors.toUnmodifiableSet());
    }

    public PrefixOption option(String group) {
        String key = normalize(group);
        if (key == null) {
            return null;
        }
        for (PrefixOption option : available) {
            if (option.group().equals(key)) {
                return option;
            }
        }
        return null;
    }

    public static String normalize(String group) {
        if (group == null || group.isBlank()) {
            return null;
        }
        return group.toLowerCase(Locale.ROOT);
    }
}
