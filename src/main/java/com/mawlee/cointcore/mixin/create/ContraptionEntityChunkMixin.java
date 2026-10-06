package com.mawlee.cointcore.mixin.create;

import com.mawlee.cointcore.claim.ContraptionClaimStallHelper;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
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

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void cointcore$stallInForeignClaim(CallbackInfo ci) {
        ContraptionClaimStallHelper.applyStall(
                (AbstractContraptionEntity) (Object) this,
                getControllingPlayer()
        );
    }
}
