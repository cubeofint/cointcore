package com.mawlee.cointcore.mixin.sfm;

import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import com.mawlee.cointcore.config.SfmPerfConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Soft-throttles SFM program execution when rolling MSPT is above the configured threshold.
 * Injects before {@code Program.tick}; the manager tick counter has already advanced.
 * <p>
 * Never hard-disables SFM: when MSPT is high, still runs 1 of every N manager ticks.
 */
@Mixin(value = ManagerBlockEntity.class, remap = false)
public abstract class ManagerBlockEntityMixin {
    @Inject(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lca/teamdman/sfml/ast/Program;tick(Lca/teamdman/sfm/common/blockentity/ManagerBlockEntity;)Z",
                    remap = false
            ),
            cancellable = true,
            remap = false
    )
    private static void cointcore$skipProgramTickWhenMsptHigh(
            Level level,
            BlockPos pos,
            BlockState state,
            ManagerBlockEntity manager,
            CallbackInfo ci
    ) {
        if (SfmPerfConfig.shouldSkipProgramTick(manager.getTick())) {
            ci.cancel();
        }
    }
}
