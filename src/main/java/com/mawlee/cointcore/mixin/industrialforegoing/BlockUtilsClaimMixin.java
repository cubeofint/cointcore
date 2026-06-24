package com.mawlee.cointcore.mixin.industrialforegoing;

import com.mawlee.cointcore.claim.ClaimGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = com.buuz135.industrial.utils.BlockUtils.class, remap = false)
public abstract class BlockUtilsClaimMixin {
    @Inject(method = "canBlockBeBrokenPlugin", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$guardPluginCheck(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!ClaimGuard.isAvailable() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity actor = ClaimGuard.resolveActor(serverLevel, pos, Optional.empty());
        if (actor != null) {
            cir.setReturnValue(ClaimGuard.canEdit(actor, level, pos));
        }
    }
}
