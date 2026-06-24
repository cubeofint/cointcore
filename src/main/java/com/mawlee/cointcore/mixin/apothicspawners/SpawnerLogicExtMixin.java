package com.mawlee.cointcore.mixin.apothicspawners;

import com.mawlee.cointcore.mixin.accessor.BaseSpawnerSpawnRangeAccessor;
import com.mawlee.cointcore.spawner.VanillaSpawnerLimits;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
        targets = "dev.shadowsoffire.apothic_spawners.block.ApothSpawnerTile$SpawnerLogicExt",
        remap = true
)
public abstract class SpawnerLogicExtMixin {
    @Inject(method = "serverTick", at = @At("HEAD"))
    private void cointcore$clampSpawnRange(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        BaseSpawnerSpawnRangeAccessor spawner = (BaseSpawnerSpawnRangeAccessor) (Object) this;
        if (spawner.cointcore$getSpawnRange() > VanillaSpawnerLimits.SPAWN_RANGE) {
            spawner.cointcore$setSpawnRange(VanillaSpawnerLimits.SPAWN_RANGE);
        }
    }
}
