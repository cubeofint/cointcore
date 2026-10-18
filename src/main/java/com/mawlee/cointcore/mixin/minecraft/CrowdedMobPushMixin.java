package com.mawlee.cointcore.mixin.minecraft;

import com.mawlee.cointcore.crowded.CrowdedMobs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Drops the O(n²) entity-push scan for mobs flagged as packed in a farm.
 */
@Mixin(LivingEntity.class)
public abstract class CrowdedMobPushMixin {
    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void cointcore$skipCrowdedPushScan(CallbackInfo ci) {
        if (CrowdedMobs.shouldDisablePush((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "doPush", at = @At("HEAD"), cancellable = true)
    private void cointcore$skipCrowdedDoPush(Entity entity, CallbackInfo ci) {
        if (CrowdedMobs.shouldSkipPushBetween((Entity) (Object) this, entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void cointcore$crowdedNotPushable(CallbackInfoReturnable<Boolean> cir) {
        if (CrowdedMobs.shouldDisablePush((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
