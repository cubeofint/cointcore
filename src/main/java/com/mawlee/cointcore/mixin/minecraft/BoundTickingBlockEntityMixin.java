package com.mawlee.cointcore.mixin.minecraft;

import com.mawlee.cointcore.watchdog.WatchdogHooks;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class BoundTickingBlockEntityMixin {
    @Shadow
    @Final
    private BlockEntity blockEntity;

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void cointcore$watchdogBlockEntityStart(CallbackInfo ci) {
        WatchdogHooks.beforeBlockEntityTick(this.blockEntity);
    }

    @Inject(method = "tick()V", at = @At("RETURN"))
    private void cointcore$watchdogBlockEntityEnd(CallbackInfo ci) {
        WatchdogHooks.afterBlockEntityTick(this.blockEntity);
    }
}
