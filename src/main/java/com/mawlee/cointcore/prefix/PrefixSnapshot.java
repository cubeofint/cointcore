package com.mawlee.cointcore.prefix;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Player prefix state used by {@link PrefixSelectionRules}. Independent of LuckPerms / Minecraft.
 *
 * <p>{@code lockedStaff} is always {@code false}: staff prefixes stay visible via stacking
 * and no longer block quest selection.
 */
public record PrefixSnapshot(
        boolean lockedStaff,
        List<PrefixOption> available,
        String selected
) {
    public PrefixSnapshot {
        lockedStaff = false;
        available = available == null ? List.of() : List.copyOf(available);
        selected = normalize(selected);
    }

    public static PrefixSnapshot of(List<PrefixOption> available, String selected) {
        return new PrefixSnapshot(false, available, selected);
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

    public PrefixOption highestQuest() {
        return PrefixSelectionRules.highestQuest(available);
    }

    public static String normalize(String group) {
        if (group == null || group.isBlank()) {
            return null;
        }
        return group.toLowerCase(Locale.ROOT);
    }
}
