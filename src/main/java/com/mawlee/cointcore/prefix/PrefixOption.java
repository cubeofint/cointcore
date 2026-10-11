package com.mawlee.cointcore.prefix;

/**
 * One selectable LuckPerms group prefix.
 *
 * @param group      lowercase group name
 * @param prefixRaw  raw LP prefix string ({@code &} / hex codes)
 * @param category   {@code quest}, {@code donation}, {@code media}, {@code builder}, {@code default}, or {@code other}
 */
public record PrefixOption(String group, String prefixRaw, String category) {
    public PrefixOption {
        group = group == null ? "" : group.toLowerCase();
        prefixRaw = prefixRaw == null ? "" : prefixRaw;
        category = category == null || category.isBlank() ? "other" : category;
    }

    public boolean hasPrefix() {
        return !prefixRaw.isBlank();
    }
}
