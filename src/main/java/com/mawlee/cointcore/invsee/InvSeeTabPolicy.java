package com.mawlee.cointcore.invsee;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pure tab visibility: which admin tabs exist given loaded mods and view rights.
 */
public final class InvSeeTabPolicy {
    public static final Set<String> GRAVE_MOD_IDS = Set.of(
            "yigd",
            "tombstone",
            "gravestone",
            "graves",
            "universal_graves"
    );

    private InvSeeTabPolicy() {
    }

    public static boolean showAccessories(boolean accessoriesLoaded) {
        return accessoriesLoaded;
    }

    public static boolean showFtb(boolean ftbEssentialsLoaded) {
        return ftbEssentialsLoaded;
    }

    public static boolean showGraves(Set<String> loadedModIds) {
        if (loadedModIds == null || loadedModIds.isEmpty()) {
            return false;
        }
        for (String id : loadedModIds) {
            if (id != null && GRAVE_MOD_IDS.contains(id.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static int mask(
            boolean canViewInventory,
            boolean canViewEnder,
            boolean accessoriesLoaded,
            boolean canViewAccessories,
            boolean ftbLoaded,
            boolean canViewFtb,
            boolean gravesLoaded,
            boolean canViewGraves,
            boolean canViewState
    ) {
        int bits = 0;
        if (canViewInventory) {
            bits |= InvSeeTab.INVENTORY.mask();
        }
        if (canViewEnder) {
            bits |= InvSeeTab.ENDER.mask();
        }
        if (showAccessories(accessoriesLoaded) && canViewAccessories) {
            bits |= InvSeeTab.ACCESSORIES.mask();
        }
        if (showFtb(ftbLoaded) && canViewFtb) {
            bits |= InvSeeTab.FTB.mask();
        }
        if (gravesLoaded && canViewGraves) {
            bits |= InvSeeTab.GRAVES.mask();
        }
        if (canViewState) {
            bits |= InvSeeTab.STATE.mask();
        }
        return bits;
    }

    public static List<InvSeeTab> visible(int mask) {
        List<InvSeeTab> tabs = new ArrayList<>();
        for (InvSeeTab tab : InvSeeTab.values()) {
            if ((mask & tab.mask()) != 0) {
                tabs.add(tab);
            }
        }
        return tabs;
    }

    public static boolean contains(int mask, InvSeeTab tab) {
        return tab != null && (mask & tab.mask()) != 0;
    }
}
