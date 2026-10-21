package com.mawlee.cointcore.mixin.ae2;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.storage.IStorageMounts;
import appeng.parts.AEBasePart;
import appeng.parts.storagebus.StorageBusPart;
import com.mawlee.cointcore.ae.Ae2PartLimits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A storage bus past its chunk limit stays in place but is inert: it mounts nothing into the network and skips its
 * periodic rescan of the attached inventory. Re-enabled automatically when the chunk drops back under the limit.
 */
@Mixin(value = StorageBusPart.class, remap = false)
public abstract class StorageBusLimitMixin {
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
