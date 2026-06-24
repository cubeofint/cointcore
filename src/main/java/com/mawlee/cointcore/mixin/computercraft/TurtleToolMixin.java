package com.mawlee.cointcore.mixin.computercraft;

import com.mawlee.cointcore.claim.ClaimGuard;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = dan200.computercraft.shared.turtle.upgrades.TurtleTool.class, remap = false)
public abstract class TurtleToolMixin {
    @Inject(method = "checkBlockBreakable", at = @At("HEAD"), cancellable = true, remap = false)
    protected void cointcore$guardBreakable(
            Level level,
            BlockPos pos,
            TurtlePlayer turtle,
            CallbackInfoReturnable<TurtleCommandResult> cir
    ) {
        if (!ClaimGuard.isAvailable()) {
            return;
        }

        ServerPlayer owner = turtle.player();
        if (owner != null && !ClaimGuard.canEdit(owner, level, pos)) {
            cir.setReturnValue(TurtleCommandResult.failure());
        }
    }
}
