package com.mawlee.cointcore.mixin.quarryplus;

import com.mawlee.cointcore.claim.ClaimGuard;
import com.yogpc.qp.machine.QuarryFakePlayerCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.yogpc.qp.machine.misc.SoftBlock$ChainBreakTask")
public abstract class SoftBlockChainBreakMixin {
    @Redirect(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"
            )
    )
    private boolean cointcore$guardRemove(Level level, BlockPos pos, boolean moveToAir) {
        if (!ClaimGuard.isAvailable() || !(level instanceof ServerLevel serverLevel)) {
            return level.removeBlock(pos, moveToAir);
        }

        ServerPlayer actor = QuarryFakePlayerCommon.getOwnImplementation(serverLevel, ServerLevel::getServer);
        if (actor != null && !ClaimGuard.canEdit(actor, level, pos)) {
            return false;
        }

        return level.removeBlock(pos, moveToAir);
    }
}
