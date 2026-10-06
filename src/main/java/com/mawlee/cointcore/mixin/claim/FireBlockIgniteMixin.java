package com.mawlee.cointcore.mixin.claim;

import com.mawlee.cointcore.claim.ClaimFlagService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireBlock.class)
public abstract class FireBlockIgniteMixin {
    @Inject(method = "getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), cancellable = true)
    private void cointcore$blockClaimFireSpread(LevelReader level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (level instanceof ServerLevel serverLevel && ClaimFlagService.blocksFireSpread(serverLevel, pos)) {
            cir.setReturnValue(0);
        }
    }
}
