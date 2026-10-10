package com.mawlee.cointcore.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lowercased searchable fields of one market listing, independent of Minecraft.
 */
public record MarketSearchTarget(
        String names,
        String tooltip,
        String modId,
        String modName,
        List<String> tags,
        String colors,
        String itemId,
        String creativeTabs
) {
    public MarketSearchTarget {
        names = lower(names);
        tooltip = lower(tooltip);
        modId = lower(modId);
        modName = lower(modName);
        tags = copyLower(tags);
        colors = lower(colors);
        itemId = lower(itemId);
        creativeTabs = lower(creativeTabs);
    }

    public static MarketSearchTarget of(
            String names,
            String tooltip,
            String modId,
            String modName,
            List<String> tags,
            String colors,
            String itemId
    ) {
        return new MarketSearchTarget(names, tooltip, modId, modName, tags, colors, itemId, modName);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static List<String> copyLower(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> copy = new ArrayList<>(values.size());
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                copy.add(value.toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(copy);
    }
}
