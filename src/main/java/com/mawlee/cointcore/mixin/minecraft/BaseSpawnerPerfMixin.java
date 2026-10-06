package com.mawlee.cointcore.mixin.minecraft;

import com.mawlee.cointcore.config.SpawnerPerfConfig;
import com.mawlee.cointcore.spawner.SpawnerPerfSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla {@link BaseSpawner} TPS: force cooldown after a fully failed spawn wave.
 * Nearby-scan caching is Apothic-only — vanilla invoke owner/signature differs.
 */
@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerPerfMixin {
    @Shadow
    private void delay(Level level, BlockPos pos) {
    }

    @Unique
    private boolean cointcore$spawnAttempted;

    @Inject(method = "serverTick", at = @At("HEAD"))
    private void cointcore$beginTick(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        cointcore$spawnAttempted = false;
        SpawnerPerfSupport.clampSpawnRates((BaseSpawner) (Object) this);
    }

    @Inject(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/BaseSpawner;getOrCreateNextSpawnData(Lnet/minecraft/world/level/Level;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/SpawnData;"
            )
    )
    private void cointcore$markSpawnAttempt(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        if (SpawnerPerfConfig.isEnabled()) {
            cointcore$spawnAttempted = true;
        }
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private void cointcore$failedSpawnCooldown(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        SpawnerPerfSupport.applyFailedSpawnCooldown(
                (BaseSpawner) (Object) this,
                level,
                pos,
                cointcore$spawnAttempted,
                this::delay
        );
    }
}
