package com.mawlee.cointcore.mixin.actuallyadditions;

import com.mawlee.cointcore.claim.ClaimGuard;
import de.ellpeck.actuallyadditions.mod.tile.TileEntityVerticalDigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(value = TileEntityVerticalDigger.class, remap = false)
public abstract class VerticalDiggerMixin {
    @Redirect(
            method = "mine",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
                    remap = true
            ),
            remap = false
    )
    private boolean cointcore$guardMineBreak(Level level, BlockPos pos, BlockState state) {
        if (ClaimGuard.isAvailable() && level instanceof ServerLevel serverLevel) {
            BlockPos diggerPos = ((BlockEntity) (Object) this).getBlockPos();
            Entity actor = ClaimGuard.resolveActor(serverLevel, diggerPos, Optional.empty());
            if (actor != null && !ClaimGuard.canEdit(actor, level, pos)) {
                return false;
            }
        }

        return level.setBlockAndUpdate(pos, state);
    }
}
