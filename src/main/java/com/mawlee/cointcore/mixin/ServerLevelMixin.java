package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.spawner.SpawnerLootInterceptor;
import com.mawlee.cointcore.spawner.SpawnerSpawnTracker;
import com.mawlee.cointcore.watchdog.WatchdogHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(
            method = "tickNonPassenger",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V")
    )
    private void cointcore$watchdogEntityTickStart(Entity entity, CallbackInfo ci) {
        WatchdogHooks.beforeEntityTick(entity);
    }

    @Inject(
            method = "tickNonPassenger",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V", shift = At.Shift.AFTER)
    )
    private void cointcore$watchdogEntityTickEnd(Entity entity, CallbackInfo ci) {
        WatchdogHooks.afterEntityTick(entity);
    }

    @Inject(method = "tryAddFreshEntityWithPassengers", at = @At("HEAD"), cancellable = true)
    private void cointcore$interceptFarmSpawnerMob(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Mob mob) || !SpawnerSpawnTracker.hasFarmSpawn(mob)) {
            return;
        }

        ServerLevel level = (ServerLevel) (Object) this;
        if (SpawnerLootInterceptor.handle(level, mob, true) == SpawnerLootInterceptor.HandleResult.CONSUMED) {
            // Report success so Apothic does not abort the spawnCount wave / delay() early.
            // The mob was discarded — it never joins the world.
            cir.setReturnValue(true);
        }
    }
}
