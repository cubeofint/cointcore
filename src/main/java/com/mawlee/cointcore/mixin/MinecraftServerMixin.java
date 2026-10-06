package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.vanish.VanishServerStatus;
import com.mawlee.cointcore.watchdog.WatchdogHooks;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "tickServer", at = @At("HEAD"))
    private void cointcore$watchdogTickStart(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        WatchdogHooks.onServerTickStart();
    }

    @Inject(method = "tickServer", at = @At("RETURN"))
    private void cointcore$watchdogTickEnd(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        WatchdogHooks.onServerTickEnd((MinecraftServer) (Object) this);
    }

    @Inject(method = "buildPlayerStatus", at = @At("RETURN"), cancellable = true)
    private void cointcore$filterVanishedFromServerList(CallbackInfoReturnable<ServerStatus.Players> cir) {
        ServerStatus.Players players = cir.getReturnValue();
        if (players == null) {
            return;
        }
        cir.setReturnValue(VanishServerStatus.filter((MinecraftServer) (Object) this, players));
    }
}
