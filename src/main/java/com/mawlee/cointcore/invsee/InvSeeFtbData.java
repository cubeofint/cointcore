package com.mawlee.cointcore.invsee;

import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

public final class InvSeeFtbData {
    private InvSeeFtbData() {
    }

    public static List<String> collect(InvSeeTarget target) {
        if (target == null) {
            return InvSeeInfoLines.ftb(false, "", List.of(), "");
        }
        return collect(target.getPlayer(), target.playerId(), target.displayName());
    }

    public static List<String> collect(Player player) {
        UUID id = player == null ? null : player.getUUID();
        String name = player == null ? "" : player.getGameProfile().getName();
        return collect(player, id, name);
    }

    public static List<String> collect(Player player, UUID playerId, String fallbackName) {
        if (!InvSeeMods.ftbEssentials()) {
            return InvSeeInfoLines.ftb(false, fallbackName, List.of(), "");
        }
        try {
            Class<?> bridge = Class.forName("com.mawlee.cointcore.invsee.integrations.FtbEssentialsInvSeeBridge");
            Object result = bridge.getMethod("collect", Player.class, UUID.class, String.class)
                    .invoke(null, player, playerId, fallbackName);
            if (result instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<String> lines = (List<String>) list;
                return lines;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return InvSeeInfoLines.ftb(true, fallbackName == null ? "" : fallbackName, List.of(), "");
    }
}
