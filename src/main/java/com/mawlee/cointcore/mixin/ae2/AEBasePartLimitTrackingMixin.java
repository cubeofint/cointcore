package com.mawlee.cointcore.mixin.ae2;

import appeng.parts.AEBasePart;
import com.mawlee.cointcore.ae.Ae2PartLimits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Re-counts limited parts in a chunk when a part joins or leaves the world (placed, loaded, broken, wrenched). */
@Mixin(value = AEBasePart.class, remap = false)
public abstract class AEBasePartLimitTrackingMixin {
    @Inject(method = "addToWorld", at = @At("TAIL"), remap = false)
    private void cointcore$partAdded(CallbackInfo ci) {
        Ae2PartLimits.onPartChanged((AEBasePart) (Object) this);
    }

    @Inject(method = "removeFromWorld", at = @At("HEAD"), remap = false)
    private void cointcore$partRemoved(CallbackInfo ci) {
        Ae2PartLimits.onPartChanged((AEBasePart) (Object) this);
    }
}
