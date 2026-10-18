package com.mawlee.cointcore.neovitae;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cheap palette scan for NeoVitae {@code NVMissingMappings#rewriteSectionPalettes}.
 * Sections whose palette has no remapped ids are skipped; matching names are rewritten
 * with a map lookup instead of a linear table walk per entry.
 */
public final class NeovitaePaletteRewrite {
    private NeovitaePaletteRewrite() {
    }

    public static boolean sectionNeedsRewrite(List<String> paletteNames, Map<String, String> remaps) {
        if (paletteNames == null || remaps == null || remaps.isEmpty()) {
            return false;
        }
        for (String name : paletteNames) {
            if (name != null && remaps.containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, String> remapTable(String[][] remaps) {
        if (remaps == null || remaps.length == 0) {
            return Map.of();
        }
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (String[] pair : remaps) {
            if (pair != null && pair.length >= 2 && pair[0] != null && pair[1] != null) {
                map.put(pair[0], pair[1]);
            }
        }
        return Map.copyOf(map);
    }
}
