package com.mawlee.cointcore.mixin.watchdog;

import com.mawlee.cointcore.watchdog.WatchdogHotFlags;
import com.mawlee.cointcore.watchdog.probes.ProbeHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelTickChunkWatchdogMixin {
    @Inject(method = "tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V", at = @At("HEAD"), require = 0)
    private void cointcore$watchdogRandomTick(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if (!WatchdogHotFlags.PROBE_BLOCK_UPDATES) {
            return;
        }
        ProbeHooks.onRandomTickChunk((ServerLevel) (Object) this, chunk, randomTickSpeed);
    }
}
