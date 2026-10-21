package com.mawlee.cointcore.mixin.extendedae;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.storage.IStorageMounts;
import appeng.parts.AEBasePart;
import com.mawlee.cointcore.ae.Ae2PartLimits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extended AE's mod / precise / tag storage buses copy AE2's storage bus instead of extending it; same inert
 * behaviour as {@code StorageBusLimitMixin}. Verified against ExtendedAE 2.2.41.
 */
@Pseudo
@Mixin(targets = "com.glodblock.github.extendedae.common.parts.base.PartSpecialStorageBus", remap = false)
public abstract class SpecialStorageBusLimitMixin {
    @Inject(method = "mountInventories", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$skipMountWhenInert(IStorageMounts mounts, CallbackInfo ci) {
        if (!Ae2PartLimits.isActive((AEBasePart) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "tickingRequest", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$idleWhenInert(IGridNode node, int ticksSinceLastCall, CallbackInfoReturnable<TickRateModulation> cir) {
        if (!Ae2PartLimits.isActive((AEBasePart) (Object) this)) {
            cir.setReturnValue(TickRateModulation.IDLE);
        }
    }
}
