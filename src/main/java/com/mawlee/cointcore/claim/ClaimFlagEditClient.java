package com.mawlee.cointcore.claim;

import dev.ftb.mods.ftbteams.api.property.TeamProperty;

public final class ClaimFlagEditClient {
    private static int mask;

    private ClaimFlagEditClient() {
    }

    public static void apply(int mask) {
        ClaimFlagEditClient.mask = mask;
    }

    public static boolean canEdit(TeamProperty<?> property) {
        Integer bit = ClaimFlagEditAccess.maskBit(property.getId().getNamespace(), property.getId().getPath());
        if (bit == null) {
            return property.isPlayerEditable();
        }
        return (mask & bit) != 0;
    }
}
