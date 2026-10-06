package com.mawlee.cointcore.mixin.sfm;

import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.program.ProgramContext;
import ca.teamdman.sfml.ast.Interval;
import com.mawlee.cointcore.config.SfmPerfConfig;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Raises short SFM timers (e.g. {@code every tick}) to at least {@link SfmPerfConfig#getMinTimerIntervalTicks()}.
 * Longer intervals such as {@code every 20 tick} keep vanilla SFM logic.
 */
@Mixin(value = Interval.class, remap = false)
public abstract class IntervalMixin {
    @Shadow
    @Final
    private int ticks;

    @Shadow
    @Final
    private int offset;

    @Shadow
    public abstract Interval.IntervalAlignment alignment();

    @Inject(method = "shouldTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$enforceMinInterval(ProgramContext context, CallbackInfoReturnable<Boolean> cir) {
        if (!SfmPerfConfig.isEnabled()) {
            return;
        }

        int min = SfmPerfConfig.getMinTimerIntervalTicks();
        if (min <= 1 || this.ticks >= min) {
            return;
        }

        ManagerBlockEntity manager = context.getManager();
        if (this.alignment() == Interval.IntervalAlignment.LOCAL) {
            cir.setReturnValue(Math.floorMod(manager.getTick(), min) == this.offset);
            return;
        }

        Level level = manager.getLevel();
        if (level == null) {
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(Math.floorMod(level.getGameTime(), min) == this.offset);
    }
}
