package com.mawlee.cointcore.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Packs lot-row labels onto one or two lines without truncating numbers.
 */
public final class MarketLotLineLayout {
    private MarketLotLineLayout() {
    }

    public static List<String> wrap(List<String> parts, int maxWidth, ToIntFunction<String> widthOf) {
        List<String> clean = new ArrayList<>();
        if (parts != null) {
            for (String part : parts) {
                if (part != null && !part.isBlank()) {
                    clean.add(part);
                }
            }
        }
        if (clean.isEmpty()) {
            return List.of();
        }
        int width = Math.max(1, maxWidth);
        String joined = join(clean);
        if (widthOf.applyAsInt(joined) <= width || clean.size() == 1) {
            return List.of(joined);
        }
        for (int split = 1; split < clean.size(); split++) {
            String first = join(clean.subList(0, split));
            String second = join(clean.subList(split, clean.size()));
            if (widthOf.applyAsInt(first) <= width && widthOf.applyAsInt(second) <= width) {
                return List.of(first, second);
            }
        }
        return List.of(clean.getFirst(), join(clean.subList(1, clean.size())));
    }

    private static String join(List<String> parts) {
        return String.join(" · ", parts);
    }
}
