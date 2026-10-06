package com.mawlee.cointcore.mixin.evilcraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.cyclops.evilcraft.entity.monster.EntityVengeanceSpirit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * EvilCraft's {@code spawnRandom} calls {@code canSpawnNew} in the while-condition up to 50 times.
 * Each call runs a full {@code foldArea} cube scan (~30k blocks). Cache the first result for the
 * duration of one {@code spawnRandom} invocation.
 */
@Mixin(value = EntityVengeanceSpirit.class, remap = false)
public abstract class EntityVengeanceSpiritSpawnRandomMixin {

    @Unique
    private static final ThreadLocal<Boolean> COINTCORE$SPAWN_ALLOWED = new ThreadLocal<>();

    @Inject(method = "spawnRandom", at = @At("HEAD"), remap = false)
    private static void cointcore$beginSpawnRandom(
            Level level,
            BlockPos blockPos,
            int area,
            CallbackInfoReturnable<EntityVengeanceSpirit> cir
    ) {
        COINTCORE$SPAWN_ALLOWED.remove();
    }

    @Inject(method = "spawnRandom", at = @At("RETURN"), remap = false)
    private static void cointcore$endSpawnRandom(
            Level level,
            BlockPos blockPos,
            int area,
            CallbackInfoReturnable<EntityVengeanceSpirit> cir
    ) {
        COINTCORE$SPAWN_ALLOWED.remove();
    }

    @WrapOperation(
            method = "spawnRandom",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/cyclops/evilcraft/entity/monster/EntityVengeanceSpirit;canSpawnNew(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z"
            ),
            remap = false
    )
    private static boolean cointcore$canSpawnNewOnce(
            Level level,
            BlockPos pos,
            Operation<Boolean> original
    ) {
        Boolean cached = COINTCORE$SPAWN_ALLOWED.get();
        if (cached != null) {
            return cached;
        }
        boolean allowed = original.call(level, pos);
        COINTCORE$SPAWN_ALLOWED.set(allowed);
        return allowed;
    }
}
