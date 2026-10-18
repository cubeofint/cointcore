package com.mawlee.cointcore.mixin.minecraft;

import com.mawlee.cointcore.crowded.CrowdedMobMarker;
import com.mawlee.cointcore.crowded.CrowdedMobs;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Caches crowded-farm state and throttles goal/target/navigation ticks.
 * Aging, egg laying, breeding timers, damage and drops stay on {@code LivingEntity.tick}.
 */
@Mixin(Mob.class)
public abstract class CrowdedMobAiMixin implements CrowdedMobMarker {
    @Unique
    private boolean cointcore$crowded;

    @Unique
    private int cointcore$nextCrowdedCheckTick;

    @Override
    public boolean cointcore$isCrowded() {
        return cointcore$crowded;
    }

    @Override
    public void cointcore$setCrowded(boolean crowded) {
        cointcore$crowded = crowded;
    }

    @Override
    public int cointcore$nextCrowdedCheckTick() {
        return cointcore$nextCrowdedCheckTick;
    }

    @Override
    public void cointcore$setNextCrowdedCheckTick(int tick) {
        cointcore$nextCrowdedCheckTick = tick;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void cointcore$refreshCrowded(CallbackInfo ci) {
        CrowdedMobs.refresh((Mob) (Object) this, this);
    }

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void cointcore$throttleCrowdedAi(CallbackInfo ci) {
        if (CrowdedMobs.shouldSkipAi((Mob) (Object) this, this)) {
            ci.cancel();
        }
    }

    @Inject(method = "playAmbientSound", at = @At("HEAD"), cancellable = true)
    private void cointcore$skipCrowdedAmbient(CallbackInfo ci) {
        if (CrowdedMobs.shouldSkipAmbient((Mob) (Object) this, this)) {
            ci.cancel();
        }
    }
}
