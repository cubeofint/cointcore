package com.mawlee.cointcore.nightvision;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class NightVisionService {
    private NightVisionService() {
    }

    public static boolean toggle(ServerPlayer player) {
        boolean enabled = !NightVisionManager.isEnabled(player);
        setEnabled(player, enabled, true);
        return enabled;
    }

    public static void setEnabled(ServerPlayer player, boolean enabled, boolean notify) {
        NightVisionManager.setEnabled(player, enabled);
        if (enabled) {
            apply(player);
            if (notify) {
                player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.NIGHT_VISION_ENABLED));
            }
        } else {
            remove(player);
            if (notify) {
                player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.NIGHT_VISION_DISABLED));
            }
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        NightVisionManager.loadFromStorage(player);
        if (!PermissionService.has(player, CointPermissionNodes.NIGHT_VISION)) {
            if (NightVisionManager.isEnabled(player)) {
                setEnabled(player, false, false);
            }
            return;
        }

        if (NightVisionManager.isEnabled(player)) {
            apply(player);
        }
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        NightVisionManager.loadFromStorage(player);
        if (NightVisionManager.isEnabled(player) && PermissionService.has(player, CointPermissionNodes.NIGHT_VISION)) {
            apply(player);
        } else {
            remove(player);
        }
    }

    public static void revokeIfUnauthorized(ServerPlayer player) {
        if (NightVisionManager.isEnabled(player) && !PermissionService.has(player, CointPermissionNodes.NIGHT_VISION)) {
            setEnabled(player, false, true);
        }
    }

    public static void reapplyIfEnabled(ServerPlayer player) {
        if (NightVisionManager.isEnabled(player)
                && PermissionService.has(player, CointPermissionNodes.NIGHT_VISION)
                && !player.hasEffect(MobEffects.NIGHT_VISION)) {
            apply(player);
        }
    }

    private static void apply(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION,
                MobEffectInstance.INFINITE_DURATION,
                0,
                false,
                false,
                true
        ));
    }

    private static void remove(ServerPlayer player) {
        player.removeEffect(MobEffects.NIGHT_VISION);
    }
}
