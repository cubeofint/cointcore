package com.mawlee.cointcore.prefix;

/**
 * One quest-track group as sent to the site catalog.
 */
public record PrefixCatalogEntry(
        String group,
        String prefixRaw,
        int priority,
        int trackIndex,
        String displayName,
        String category
) {
    public PrefixCatalogEntry {
        group = PrefixSnapshot.normalize(group) == null ? "" : PrefixSnapshot.normalize(group);
        prefixRaw = prefixRaw == null ? "" : prefixRaw;
        displayName = displayName == null ? "" : displayName;
        category = category == null || category.isBlank() ? PrefixCatalog.CATEGORY : category;
    }
}
