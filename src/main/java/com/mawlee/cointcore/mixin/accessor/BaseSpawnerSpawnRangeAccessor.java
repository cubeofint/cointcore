package com.mawlee.cointcore.mixin.accessor;

import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BaseSpawner.class)
public interface BaseSpawnerSpawnRangeAccessor {
    @Accessor("spawnRange")
    int cointcore$getSpawnRange();

    @Accessor("spawnRange")
    void cointcore$setSpawnRange(int spawnRange);
}
