package com.mawlee.cointcore.radio;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

public final class CointRadioProtection {

    private CointRadioProtection() {
    }

    public static boolean canUseRadio(ServerPlayer player, BlockPos pos, InteractionHand hand) {
        return player != null && pos != null
                && com.mawlee.cointcore.claim.ClaimGuard.canInteract(player, player.level(), pos);
    }

    public static boolean denyWithMessage(ServerPlayer player) {
        if (player == null) {
            return true;
        }

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§c[CointMusic] Это радио находится в чужом привате."),
                true
        );

        return true;
    }
}
