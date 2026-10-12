package com.mawlee.cointcore.prefix;

import java.util.Objects;

/**
 * Own-user quest prefix nodes we manage (selection meta + render PrefixNode).
 * Used to skip LuckPerms writes when the desired state already matches.
 */
public record QuestPrefixState(
        String selectedGroup,
        String prefixRaw,
        int ourPrefixNodes,
        int ourMetaNodes,
        boolean staleMeta,
        boolean stalePriority
) {
    public QuestPrefixState {
        selectedGroup = PrefixSnapshot.normalize(selectedGroup);
        prefixRaw = blankToNull(prefixRaw);
        ourPrefixNodes = Math.max(0, ourPrefixNodes);
        ourMetaNodes = Math.max(0, ourMetaNodes);
    }

    public static QuestPrefixState desired(String selectedGroup, String prefixRaw) {
        String group = PrefixSnapshot.normalize(selectedGroup);
        String prefix = blankToNull(prefixRaw);
        return new QuestPrefixState(
                group,
                prefix,
                prefix == null ? 0 : 1,
                group == null ? 0 : 1,
                false,
                false
        );
    }

    public static QuestPrefixState current(
            String selectedGroup,
            String prefixRaw,
            int ourPrefixNodes,
            int ourMetaNodes
    ) {
        return current(selectedGroup, prefixRaw, ourPrefixNodes, ourMetaNodes, false, false);
    }

    public static QuestPrefixState current(
            String selectedGroup,
            String prefixRaw,
            int ourPrefixNodes,
            int ourMetaNodes,
            boolean staleMeta
    ) {
        return current(selectedGroup, prefixRaw, ourPrefixNodes, ourMetaNodes, staleMeta, false);
    }

    public static QuestPrefixState current(
            String selectedGroup,
            String prefixRaw,
            int ourPrefixNodes,
            int ourMetaNodes,
            boolean staleMeta,
            boolean stalePriority
    ) {
        return new QuestPrefixState(
                selectedGroup,
                prefixRaw,
                ourPrefixNodes,
                ourMetaNodes,
                staleMeta,
                stalePriority
        );
    }

    public boolean needsWrite(QuestPrefixState desired) {
        if (desired == null) {
            return false;
        }
        return staleMeta
                || stalePriority
                || ourPrefixNodes != desired.ourPrefixNodes
                || ourMetaNodes != desired.ourMetaNodes
                || !Objects.equals(selectedGroup, desired.selectedGroup)
                || !Objects.equals(prefixRaw, desired.prefixRaw);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
