package com.mawlee.cointcore.privilege.integrations;

import com.mawlee.cointcore.privilege.FlightItemMatcher;
import net.minecraft.server.level.ServerPlayer;
import top.theillusivec4.curios.api.CuriosApi;

public final class CuriosFlightScanner {
    private CuriosFlightScanner() {
    }

    public static boolean hasFlightItemEquipped(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.isEquipped(FlightItemMatcher::isFlightItem))
                .orElse(false);
    }
}
