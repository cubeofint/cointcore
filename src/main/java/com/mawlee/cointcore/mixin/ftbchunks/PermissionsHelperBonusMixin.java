package com.mawlee.cointcore.mixin.ftbchunks;

import com.mawlee.cointcore.ftb.ChunkBonusService;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = dev.ftb.mods.ftbchunks.integration.PermissionsHelper.class, remap = false)
public abstract class PermissionsHelperBonusMixin {
    @Inject(method = "getMaxClaimedChunks", at = @At("RETURN"), cancellable = true, remap = false)
    private static void cointcore$addBonusClaimed(
            ServerPlayer player,
            int defaultValue,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (!ChunkBonusService.isActive() || player == null) {
            return;
        }

        int bonus = ChunkBonusService.getBonusClaimedChunks(player);
        if (bonus > 0) {
            cir.setReturnValue(cir.getReturnValue() + bonus);
        }
    }

    @Inject(method = "getMaxForceLoadedChunks", at = @At("RETURN"), cancellable = true, remap = false)
    private static void cointcore$addBonusForceLoaded(
            ServerPlayer player,
            int defaultValue,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (!ChunkBonusService.isActive() || player == null) {
            return;
        }

        int bonus = ChunkBonusService.getBonusForceLoadedChunks(player);
        if (bonus > 0) {
            cir.setReturnValue(cir.getReturnValue() + bonus);
        }
    }
}
