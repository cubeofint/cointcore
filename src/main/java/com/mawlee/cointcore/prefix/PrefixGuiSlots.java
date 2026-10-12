package com.mawlee.cointcore.prefix;

import java.util.ArrayList;
import java.util.List;

/**
 * Chest-menu slot map for {@link PrefixGui}. Slot 0 is auto.
 * {@link java.util.List#copyOf} rejects nulls, so auto is an empty string.
 */
public final class PrefixGuiSlots {
    public static final String AUTO = "";
    public static final int MENU_SIZE = 27;

    private PrefixGuiSlots() {
    }

    public static List<String> of(List<String> groups) {
        List<String> slots = new ArrayList<>();
        slots.add(AUTO);
        if (groups != null) {
            for (String group : groups) {
                if (slots.size() >= MENU_SIZE) {
                    break;
                }
                if (group != null && !group.isBlank()) {
                    slots.add(group);
                }
            }
        }
        return List.copyOf(slots);
    }

    public static boolean isAuto(String group) {
        return group == null || group.isBlank();
    }
}
