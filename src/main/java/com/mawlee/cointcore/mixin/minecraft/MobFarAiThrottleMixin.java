package com.mawlee.cointcore.mixin.minecraft;

import com.mawlee.cointcore.config.EntityPerfConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mobs with no player nearby (farms kept loaded by chunk loaders) think only every {@code interval} ticks: their
 * {@code serverAiStep} (sensing, goals, navigation, move/look/jump control) is skipped in between. Physics, damage,
 * death and despawn stay every tick. Mobs that ride or carry something, have a target, or are excluded by type are
 * never slowed.
 */
@Mixin(Mob.class)
public abstract class MobFarAiThrottleMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void cointcore$skipFarAi(CallbackInfo ci) {
        if (!EntityPerfConfig.farAiEnabled()) {
            return;
        }
        Mob self = (Mob) (Object) this;
        int interval = EntityPerfConfig.farAiInterval();
        if (interval <= 1 || (self.tickCount + self.getId()) % interval == 0) {
            return;
        }
        if (!(self.level() instanceof ServerLevel level) || self.isPassenger() || self.isVehicle()
                || self.getTarget() != null || EntityPerfConfig.farAiExcluded(self.getType())) {
            return;
        }
        double limit = EntityPerfConfig.farAiDistanceSq();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.distanceToSqr(self) < limit) {
                return;
            }
        }
        ci.cancel();
    }
}
