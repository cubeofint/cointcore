package com.mawlee.cointcore.invsee;

import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class InvSeeFtbData {
    private InvSeeFtbData() {
    }

    public static List<String> collect(Player player) {
        if (!InvSeeMods.ftbEssentials() || player == null) {
            return InvSeeInfoLines.ftb(false, "", List.of(), "");
        }
        try {
            Class<?> bridge = Class.forName("com.mawlee.cointcore.invsee.integrations.FtbEssentialsInvSeeBridge");
            Object result = bridge.getMethod("collect", Player.class).invoke(null, player);
            if (result instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<String> lines = (List<String>) list;
                return lines;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return InvSeeInfoLines.ftb(true, "", List.of(), "");
    }
}
