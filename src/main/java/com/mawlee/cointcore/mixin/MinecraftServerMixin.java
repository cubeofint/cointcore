package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.vanish.VanishServerStatus;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "buildPlayerStatus", at = @At("RETURN"), cancellable = true)
    private void cointcore$filterVanishedFromServerList(CallbackInfoReturnable<ServerStatus.Players> cir) {
        ServerStatus.Players players = cir.getReturnValue();
        if (players == null) {
            return;
        }
        cir.setReturnValue(VanishServerStatus.filter((MinecraftServer) (Object) this, players));
    }
}
