package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.keepinventory.KeepInventoryService;
import com.mawlee.cointcore.spawner.SpawnerLootCapture;
import com.mawlee.cointcore.vanish.VanishManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    private LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "canBeSeenByAnyone", at = @At("HEAD"), cancellable = true)
    private void cointcore$hideVanishedFromMobs(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player
                && VanishManager.isVanished(player)
                && !VanishManager.canMobsTarget(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void cointcore$blockGlowingEffectWhileVanished(
            MobEffectInstance effectInstance,
            Entity source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if ((Object) this instanceof ServerPlayer player
                && VanishManager.isVanished(player)
                && effectInstance.getEffect().is(MobEffects.GLOWING)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "forceAddEffect", at = @At("HEAD"), cancellable = true)
    private void cointcore$blockForcedGlowingEffectWhileVanished(
            MobEffectInstance effectInstance,
            Entity source,
            CallbackInfo ci
    ) {
        if ((Object) this instanceof ServerPlayer player
                && VanishManager.isVanished(player)
                && effectInstance.getEffect().is(MobEffects.GLOWING)) {
            ci.cancel();
        }
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void cointcore$skipDeathLootForDonor(ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player && KeepInventoryService.shouldBlockDeathDrops(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
    private void cointcore$skipExperienceDuringLootCapture(CallbackInfo ci) {
        if (SpawnerLootCapture.isActive()) {
            ci.cancel();
        }
    }
}
