package com.mawlee.cointcore.mixin.fluxnetworks;

import com.mawlee.cointcore.flux.FluxAdminAccess;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sonar.fluxnetworks.common.data.FluxPlayerData;

/**
 * Treat {@code cointcore.flux.admin} as Flux Super Admin: edit any network and
 * already-connected devices without owning them or using the Admin Configurator.
 */
@Mixin(value = FluxPlayerData.class, remap = false)
public abstract class FluxPlayerDataMixin {
    @Inject(method = "isPlayerSuperAdmin", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$permissionIsSuperAdmin(
            ServerPlayer player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (FluxAdminAccess.canModifyAny(player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canActivateSuperAdmin", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$permissionCanActivateSuperAdmin(
            ServerPlayer player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (FluxAdminAccess.canModifyAny(player)) {
            cir.setReturnValue(true);
        }
    }
}
