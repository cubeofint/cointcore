package com.mawlee.cointcore.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.seeinvisible.SeeInvisibleClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Staff with see-invisible pierce the model, but vanilla still spawns white
 * invisibility swirls from {@code DATA_EFFECT_PARTICLES}. Suppress those locally.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntitySeeInvisibleParticlesMixin {
    @WrapOperation(
            method = "tickEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
            )
    )
    private void cointcore$hideInvisibilityParticlesForStaff(
            Level level,
            ParticleOptions particle,
            double x,
            double y,
            double z,
            double xSpeed,
            double ySpeed,
            double zSpeed,
            Operation<Void> original
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (level.isClientSide() && self instanceof Player && self.isInvisible()) {
            Player local = Minecraft.getInstance().player;
            if (SeeInvisibleClient.canSeeInvisible(local)) {
                return;
            }
        }
        original.call(level, particle, x, y, z, xSpeed, ySpeed, zSpeed);
    }
}
