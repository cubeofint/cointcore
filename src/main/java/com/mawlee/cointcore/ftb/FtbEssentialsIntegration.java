package com.mawlee.cointcore.ftb;

import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

public final class FtbEssentialsIntegration {
    private FtbEssentialsIntegration() {
    }

    public static boolean isAvailable() {
        return LoadingModList.get().getMods().stream()
                .map(ModInfo::getModId)
                .anyMatch("ftbessentials"::equals);
    }
}
