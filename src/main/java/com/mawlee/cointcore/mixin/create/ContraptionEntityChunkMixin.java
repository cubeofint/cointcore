package com.mawlee.cointcore.mixin.create;

import com.mawlee.cointcore.claim.ClaimGuard;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(value = AbstractContraptionEntity.class, remap = false)
public abstract class ContraptionEntityChunkMixin {
    @Shadow(remap = false)
    public abstract Optional<UUID> getControllingPlayer();

    @Shadow(remap = false)
    public abstract Contraption getContraption();

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void cointcore$stallInForeignClaim(CallbackInfo ci) {
        if (!ClaimGuard.isAvailable()) {
            return;
        }

        AbstractContraptionEntity self = (AbstractContraptionEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }

        Contraption contraption = getContraption();
        if (contraption == null) {
            return;
        }

        Entity actor = ClaimGuard.resolveActor(level, self.blockPosition(), getControllingPlayer());
        if (actor == null) {
            return;
        }

        if (!ClaimGuard.canEdit(actor, level, self.blockPosition())) {
            contraption.stalled = true;
        }
    }
}
