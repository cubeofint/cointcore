package com.mawlee.cointcore.mixin.watchdog;

import com.mawlee.cointcore.watchdog.probes.ProbeHooks;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerSaveWatchdogMixin {
    @Inject(method = "saveEverything(ZZZ)Z", at = @At("HEAD"), require = 0)
    private void cointcore$watchdogSaveEverythingStart(
            boolean suppressLog,
            boolean flush,
            boolean force,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ProbeHooks.onWorldSaveStart();
    }

    @Inject(method = "saveEverything(ZZZ)Z", at = @At("RETURN"), require = 0)
    private void cointcore$watchdogSaveEverythingEnd(
            boolean suppressLog,
            boolean flush,
            boolean force,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ProbeHooks.onWorldSaveEnd("everything");
    }

    @Inject(method = "saveAllChunks(ZZZ)Z", at = @At("HEAD"), require = 0)
    private void cointcore$watchdogSaveChunksStart(
            boolean suppressLog,
            boolean flush,
            boolean force,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ProbeHooks.onWorldSaveStart();
    }

    @Inject(method = "saveAllChunks(ZZZ)Z", at = @At("RETURN"), require = 0)
    private void cointcore$watchdogSaveChunksEnd(
            boolean suppressLog,
            boolean flush,
            boolean force,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ProbeHooks.onWorldSaveEnd("chunks");
    }
}
