package com.mawlee.cointcore.mixin.create;

import com.mawlee.cointcore.claim.ContraptionClaimStallHelper;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CarriageContraptionEntity.class, remap = false)
public abstract class CarriageContraptionEntityMixin {
    @Inject(method = "tickContraption", at = @At("HEAD"), remap = false)
    private void cointcore$stallTrainInForeignClaim(CallbackInfo ci) {
        AbstractContraptionEntity self = (AbstractContraptionEntity) (Object) this;
        ContraptionClaimStallHelper.applyStall(self, self.getControllingPlayer());
    }
}
