package com.mawlee.cointcore.mixin.iceandfire;

import com.iafenvoy.iceandfire.entity.util.BlockLaunchExplosion;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.claim.ClaimGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BlockLaunchExplosion.class, remap = false)
public abstract class BlockLaunchExplosionMixin {
    @WrapOperation(
            method = "finalizeExplosion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            ),
            remap = false
    )
    private boolean cointcore$guardLaunchedBlock(
            Level level,
            BlockPos pos,
            BlockState state,
            int flags,
            Operation<Boolean> original
    ) {
        Entity source = ((Explosion) (Object) this).getDirectSourceEntity();
        if (!ClaimGuard.canMobGriefAt(source, level, pos)) {
            return false;
        }
        return original.call(level, pos, state, flags);
    }
}
