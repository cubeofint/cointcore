package com.mawlee.cointcore.vanish;

import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class VanishVisibility {
    private VanishVisibility() {
    }

    public static boolean canSeeVanished(ServerPlayer viewer) {
        return PermissionService.has(viewer, CointPermissionNodes.SEE_VANISHED);
    }

    public static boolean isHiddenFrom(Entity subject, Entity vanishedPlayer) {
        if (!(vanishedPlayer instanceof ServerPlayer vanished)) {
            return false;
        }

        if (!(subject instanceof ServerPlayer viewer)) {
            return VanishManager.isVanished(vanished);
        }

        return VanishManager.shouldHideFrom(vanished, viewer);
    }

    public static boolean isHiddenFrom(ServerPlayer viewer, ServerPlayer vanished) {
        return VanishManager.shouldHideFrom(vanished, viewer);
    }
}
