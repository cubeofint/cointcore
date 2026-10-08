package com.mawlee.cointcore.invsee;

import net.neoforged.fml.ModList;

import java.util.HashSet;
import java.util.Set;

public final class InvSeeMods {
    private InvSeeMods() {
    }

    public static boolean curios() {
        return loaded("curios");
    }

    public static boolean accessories() {
        return loaded("accessories");
    }

    public static boolean ftbEssentials() {
        return loaded("ftbessentials");
    }

    public static boolean graves() {
        return InvSeeTabPolicy.showGraves(loadedIds());
    }

    public static boolean loaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static Set<String> loadedIds() {
        Set<String> ids = new HashSet<>();
        ModList.get().getMods().forEach(mod -> ids.add(mod.getModId()));
        return ids;
    }
}
