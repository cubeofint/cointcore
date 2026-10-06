package com.mawlee.cointcore.mixin.oritech;

import com.mawlee.cointcore.config.ExplosionTerrainConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rearth.oritech.block.entity.reactor.NuclearExplosionEntity;

/**
 * Oritech nukes skip {@code ExplosionEvent} and instead tick
 * {@link NuclearExplosionEntity}: Manhattan spheres, directional waves, then
 * {@code processBorderBlocks} (grass/stone/wood replacements). Terrain protection
 * must cancel that path or the nuke still "processes" without a crater.
 */
@Mixin(value = NuclearExplosionEntity.class, remap = false)
public abstract class NuclearExplosionEntityMixin {
    @Inject(
            method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lrearth/oritech/block/entity/reactor/NuclearExplosionEntity;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void cointcore$skipNukeTerrain(
            Level world,
            BlockPos pos,
            BlockState state,
            NuclearExplosionEntity blockEntity,
            CallbackInfo ci
    ) {
        if (!ExplosionTerrainConfig.isEnabled() || world.isClientSide) {
            return;
        }
        world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        ci.cancel();
    }
}
