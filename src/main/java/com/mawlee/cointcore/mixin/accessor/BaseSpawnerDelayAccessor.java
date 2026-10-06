package com.mawlee.cointcore.mixin.accessor;

import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BaseSpawner.class)
public interface BaseSpawnerDelayAccessor {
    @Accessor("spawnDelay")
    int cointcore$getSpawnDelay();

    @Accessor("spawnDelay")
    void cointcore$setSpawnDelay(int spawnDelay);
}
