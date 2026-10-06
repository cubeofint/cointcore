package com.mawlee.cointcore.mixin.justdirethings;

import com.direwolf20.justdirethings.util.MiscTools;
import com.mawlee.cointcore.tickaccel.TickAccelerationDeny;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Broad Time Wand deny: datapack tags + spawner type + path heuristics (altar/boss/ritual/…).
 * Vanilla JDT only checks empty {@code justdirethings:tick_speed_deny}.
 */
@Mixin(value = MiscTools.class, remap = false)
public abstract class MiscToolsTickAccelMixin {

    @Inject(
            method = "isValidTickAccelBlock",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private static void cointcore$denyTickAccel(
            ServerLevel serverLevel,
            BlockState blockState,
            BlockEntity blockEntity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        if (TickAccelerationDeny.isDenied(blockState)) {
            cir.setReturnValue(false);
        }
    }
}
