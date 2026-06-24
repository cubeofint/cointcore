package com.mawlee.cointcore.mixin.minecraft;

import com.mawlee.cointcore.claim.ClaimGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Level.class)
public abstract class LevelBlockMutationGuardMixin {
    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cointcore$guardSetBlock(
            BlockPos pos,
            BlockState state,
            int flags,
            int recursionLeft,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!((Object) this instanceof ServerLevel level)) {
            return;
        }

        if (!ClaimGuard.shouldGuardBlockMutations()) {
            return;
        }

        if (!ClaimGuard.canEditAtCurrentActor(level, pos, Optional.empty())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "removeBlock(Lnet/minecraft/core/BlockPos;Z)Z", at = @At("HEAD"), cancellable = true)
    private void cointcore$guardRemoveBlock(BlockPos pos, boolean moveToAir, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerLevel level)) {
            return;
        }

        if (!ClaimGuard.shouldGuardBlockMutations()) {
            return;
        }

        if (!ClaimGuard.canEditAtCurrentActor(level, pos, Optional.empty())) {
            cir.setReturnValue(false);
        }
    }
}
