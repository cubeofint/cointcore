package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.slots.ReservedSlotsService;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(PlayerList.class)
public abstract class PlayerListReservedSlotsMixin {
    @Shadow
    private int maxPlayers;

    @Inject(method = "getMaxPlayers", at = @At("HEAD"), cancellable = true)
    private void cointcore$raiseMaxPlayersForReservedSlots(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(ReservedSlotsService.effectiveMaxPlayers(maxPlayers));
    }

    @Inject(method = "canPlayerLogin", at = @At("RETURN"), cancellable = true)
    private void cointcore$enforceReservedSlots(
            SocketAddress address,
            GameProfile profile,
            CallbackInfoReturnable<Component> cir
    ) {
        Component denial = ReservedSlotsService.loginDenial((PlayerList) (Object) this, profile, cir.getReturnValue());
        cir.setReturnValue(denial);
    }
}
