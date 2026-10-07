package com.mawlee.cointcore.invsee;

import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

public final class InvSeeGravesData {
    private InvSeeGravesData() {
    }

    public static List<String> collect(Player player, UUID playerId) {
        String lastDeath = InvSeeStateCollector.lastDeath(player);
        boolean present = InvSeeMods.graves();
        if (!present) {
            return InvSeeInfoLines.graves(false, lastDeath, List.of());
        }
        List<String> graves = List.of();
        try {
            Class<?> bridge = Class.forName("com.mawlee.cointcore.invsee.integrations.GravesInvSeeBridge");
            Object result = bridge.getMethod("listGraves", Player.class, UUID.class).invoke(null, player, playerId);
            if (result instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<String> typed = (List<String>) list;
                graves = typed;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return InvSeeInfoLines.graves(true, lastDeath, graves);
    }
}
