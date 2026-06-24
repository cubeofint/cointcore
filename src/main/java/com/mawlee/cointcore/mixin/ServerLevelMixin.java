package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.spawner.SpawnerLootInterceptor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tryAddFreshEntityWithPassengers", at = @At("HEAD"), cancellable = true)
    private void cointcore$interceptPlayerSpawnerMob(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Mob mob)) {
            return;
        }

        ServerLevel level = (ServerLevel) (Object) this;
        if (SpawnerLootInterceptor.handle(level, mob, true) == SpawnerLootInterceptor.HandleResult.CONSUMED) {
            cir.setReturnValue(false);
        }
    }
}
