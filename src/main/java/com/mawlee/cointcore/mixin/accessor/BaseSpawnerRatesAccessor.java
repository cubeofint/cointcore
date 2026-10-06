package com.mawlee.cointcore.mixin.accessor;

import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BaseSpawner.class)
public interface BaseSpawnerRatesAccessor {
    @Accessor("minSpawnDelay")
    int cointcore$getMinSpawnDelay();

    @Accessor("minSpawnDelay")
    void cointcore$setMinSpawnDelay(int minSpawnDelay);

    @Accessor("maxSpawnDelay")
    int cointcore$getMaxSpawnDelay();

    @Accessor("maxSpawnDelay")
    void cointcore$setMaxSpawnDelay(int maxSpawnDelay);

    @Accessor("spawnCount")
    int cointcore$getSpawnCount();

    @Accessor("spawnCount")
    void cointcore$setSpawnCount(int spawnCount);
}
