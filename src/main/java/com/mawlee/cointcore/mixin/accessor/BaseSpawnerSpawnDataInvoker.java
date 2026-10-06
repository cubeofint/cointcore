package com.mawlee.cointcore.mixin.accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import org.jetbrains.annotations.Nullable;

@Mixin(BaseSpawner.class)
public interface BaseSpawnerSpawnDataInvoker {
    @Invoker("getOrCreateNextSpawnData")
    SpawnData cointcore$getOrCreateNextSpawnData(
            @Nullable Level level,
            RandomSource random,
            BlockPos pos
    );
}
