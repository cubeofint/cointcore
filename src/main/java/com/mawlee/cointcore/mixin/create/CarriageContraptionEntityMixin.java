package com.mawlee.cointcore.mixin.create;

import com.mawlee.cointcore.claim.ClaimGuard;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CarriageContraptionEntity.class, remap = false)
public abstract class CarriageContraptionEntityMixin {
    @Inject(method = "tickContraption", at = @At("HEAD"), remap = false)
    private void cointcore$stallTrainInForeignClaim(CallbackInfo ci) {
        if (!ClaimGuard.isAvailable()) {
            return;
        }

        AbstractContraptionEntity self = (AbstractContraptionEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }

        Contraption contraption = self.getContraption();
        if (contraption == null) {
            return;
        }

        Entity actor = ClaimGuard.resolveActor(level, self.blockPosition(), self.getControllingPlayer());
        if (actor == null) {
            return;
        }

        if (!ClaimGuard.canEdit(actor, level, self.blockPosition())) {
            contraption.stalled = true;
        }
    }
}
