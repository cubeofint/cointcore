package com.mawlee.cointcore.prefix;

/**
 * One selectable LuckPerms quest-group prefix.
 *
 * @param group      lowercase group name
 * @param prefixRaw  raw LP prefix string ({@code &} / hex codes)
 * @param category   always {@code quest} for selectable rows
 * @param priority   own PrefixNode priority (no inheritance)
 * @param trackIndex index on the LuckPerms {@code ranks} track
 */
public record PrefixOption(String group, String prefixRaw, String category, int priority, int trackIndex) {
    public PrefixOption {
        group = group == null ? "" : group.toLowerCase();
        prefixRaw = prefixRaw == null ? "" : prefixRaw;
        category = category == null || category.isBlank() ? "quest" : category;
    }

    public PrefixOption(String group, String prefixRaw, String category) {
        this(group, prefixRaw, category, 0, 0);
    }

    public boolean hasPrefix() {
        return !prefixRaw.isBlank();
    }
}
