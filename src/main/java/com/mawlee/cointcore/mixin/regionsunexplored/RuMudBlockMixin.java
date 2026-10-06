package com.mawlee.cointcore.mixin.regionsunexplored;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.regions_unexplored.world.level.block.other_dirt.RuMudBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Peat/silt mud register {@code .randomTicks()} only to rarely convert to clay above dripstone.
 * That puts huge biome surfaces into the random-tick lottery for almost no effect — disable it.
 */
@Mixin(value = RuMudBlock.class, remap = false)
public abstract class RuMudBlockMixin extends Block {
    private RuMudBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return false;
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$cancelRandomTick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci
    ) {
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$cancelScheduledTick(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}
