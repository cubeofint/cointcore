package com.mawlee.cointcore.prefix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure prefix-selection policy: only quest-track groups are selectable.
 * Staff / donate / media / builder prefixes stay visible via LuckPerms stacking
 * and never lock the picker.
 */
public final class PrefixSelectionRules {
    public static final String REASON_UNAVAILABLE = "unavailable";
    public static final String REASON_AUTO = "auto";
    public static final String REASON_APPLIED = "applied";
    public static final String REASON_UNCHANGED = "unchanged";
    public static final String REASON_GROUP_LOST = "group_lost";

    public enum Kind {
        APPLIED,
        AUTO,
        UNAVAILABLE,
        UNCHANGED
    }

    public record Decision(Kind kind, String selectedGroup, boolean clearOverride, String reason) {
        public Decision {
            if (kind == null) {
                throw new IllegalArgumentException("kind");
            }
            selectedGroup = PrefixSnapshot.normalize(selectedGroup);
            reason = reason == null ? "" : reason;
        }

        public static Decision applied(String group) {
            return new Decision(Kind.APPLIED, group, false, REASON_APPLIED);
        }

        public static Decision auto() {
            return new Decision(Kind.AUTO, null, true, REASON_AUTO);
        }

        public static Decision groupLost() {
            return new Decision(Kind.AUTO, null, true, REASON_GROUP_LOST);
        }

        public static Decision unavailable(String group) {
            return new Decision(Kind.UNAVAILABLE, PrefixSnapshot.normalize(group), false, REASON_UNAVAILABLE);
        }

        public static Decision unchanged(String selected) {
            return new Decision(Kind.UNCHANGED, selected, false, REASON_UNCHANGED);
        }
    }

    private PrefixSelectionRules() {
    }

    /**
     * Selectable groups: quest-track groups the player actually has, with a prefix, minus excludes.
     */
    public static List<PrefixOption> available(
            Collection<PrefixOption> heldWithPrefix,
            Collection<String> questGroups,
            Collection<String> excludeGroups
    ) {
        Set<String> quest = normalizeSet(questGroups);
        Set<String> exclude = normalizeSet(excludeGroups);
        List<PrefixOption> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        if (heldWithPrefix == null || quest.isEmpty()) {
            return List.of();
        }
        for (PrefixOption option : heldWithPrefix) {
            if (option == null || !option.hasPrefix()) {
                continue;
            }
            String group = option.group();
            if (group.isBlank() || !quest.contains(group) || exclude.contains(group) || !seen.add(group)) {
                continue;
            }
            result.add(option);
        }
        return List.copyOf(result);
    }

    public static Decision decideSet(Collection<String> availableGroups, String requestedGroup) {
        String group = PrefixSnapshot.normalize(requestedGroup);
        if (group == null) {
            return Decision.auto();
        }
        if (!normalizeSet(availableGroups).contains(group)) {
            return Decision.unavailable(group);
        }
        return Decision.applied(group);
    }

    public static Decision decideAuto() {
        return Decision.auto();
    }

    /**
     * After a LuckPerms recalc: a selected quest group that is gone falls back to auto (highest quest).
     */
    public static Decision reconcile(String selected, Collection<String> availableGroups) {
        String current = PrefixSnapshot.normalize(selected);
        if (current == null) {
            return Decision.unchanged(null);
        }
        if (!normalizeSet(availableGroups).contains(current)) {
            return Decision.groupLost();
        }
        return Decision.unchanged(current);
    }

    /**
     * Highest quest rank among selectable options: max own-prefix priority, then later track index.
     */
    public static PrefixOption highestQuest(Collection<PrefixOption> available) {
        if (available == null || available.isEmpty()) {
            return null;
        }
        PrefixOption best = null;
        for (PrefixOption option : available) {
            if (option == null || !option.hasPrefix()) {
                continue;
            }
            if (best == null
                    || option.priority() > best.priority()
                    || option.priority() == best.priority() && option.trackIndex() > best.trackIndex()) {
                best = option;
            }
        }
        return best;
    }

    public static String category(
            String group,
            Collection<String> ranksTrack,
            Collection<String> donationGroups,
            Collection<String> mediaGroups,
            Collection<String> builderGroups,
            String defaultGroup,
            String unmatchedCategory
    ) {
        String key = PrefixSnapshot.normalize(group);
        if (key == null) {
            return "other";
        }
        if (key.equals(PrefixSnapshot.normalize(defaultGroup))) {
            return "default";
        }
        if (normalizeSet(ranksTrack).contains(key)) {
            return "quest";
        }
        if (normalizeSet(mediaGroups).contains(key)) {
            return "media";
        }
        if (normalizeSet(builderGroups).contains(key)) {
            return "builder";
        }
        if (normalizeSet(donationGroups).contains(key)) {
            return "donation";
        }
        return unmatchedCategory == null || unmatchedCategory.isBlank() ? "other" : unmatchedCategory;
    }

    static Set<String> normalizeSet(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = PrefixSnapshot.normalize(value);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }

    public static String describe(Kind kind) {
        return switch (kind) {
            case APPLIED -> REASON_APPLIED;
            case AUTO -> REASON_AUTO;
            case UNAVAILABLE -> REASON_UNAVAILABLE;
            case UNCHANGED -> REASON_UNCHANGED;
        };
    }
}
