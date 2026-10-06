package com.mawlee.cointcore.mixin.evilcraft;

import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.cyclops.evilcraft.entity.item.EntityBroom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Smash part also asks the client to send {@code STOP_DESTROY_BLOCK}.
 * The server treats that as a normal dig, so the broom tick cancel alone does not stop it.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class BroomMountedDigMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void cointcore$noDigWhileRidingBroom(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        ServerboundPlayerActionPacket.Action action = packet.getAction();
        if (action != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
                && action != ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK
                && action != ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
            return;
        }
        if (player.getVehicle() instanceof EntityBroom) {
            ci.cancel();
        }
    }
}
