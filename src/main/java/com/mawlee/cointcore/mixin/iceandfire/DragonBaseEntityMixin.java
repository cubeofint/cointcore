package com.mawlee.cointcore.mixin.iceandfire;

import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.mawlee.cointcore.claim.ClaimGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dragons crush blocks inside their AABB via {@code breakBlock}, which never
 * goes through {@code IafDragonDestructionManager}.
 */
@Mixin(value = DragonBaseEntity.class, remap = false)
public abstract class DragonBaseEntityMixin {
    @Inject(method = "breakBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$guardHitboxBreak(BlockPos pos, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!ClaimGuard.canMobGriefAt(self, self.level(), pos)) {
            ci.cancel();
        }
    }
}
