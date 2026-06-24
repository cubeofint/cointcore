package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.ignore.IgnoreService;
import com.mawlee.cointcore.join.JoinLeaveMessageFilter;
import com.mawlee.cointcore.vanish.VanishPacketFilter;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {
    @Shadow
    @Final
    protected MinecraftServer server;

    @ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), argsOnly = true)
    private Packet<?> cointcore$sanitizeOutgoingPacket(Packet<?> packet) {
        if (!((Object) this instanceof ServerGamePacketListenerImpl)) {
            return packet;
        }
        return VanishPacketFilter.sanitize(server, packet);
    }

    @ModifyVariable(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Packet<?> cointcore$sanitizeOutgoingPacketWithListener(Packet<?> packet) {
        if (!((Object) this instanceof ServerGamePacketListenerImpl)) {
            return packet;
        }
        return VanishPacketFilter.sanitize(server, packet);
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void cointcore$filterOutgoingPackets(Packet<?> packet, CallbackInfo ci) {
        if (!((Object) this instanceof ServerGamePacketListenerImpl listener)) {
            return;
        }

        if (JoinLeaveMessageFilter.shouldSuppressOutgoingPacket(packet)) {
            ci.cancel();
            return;
        }

        if (VanishPacketFilter.shouldCancel(server, listener.player, packet)) {
            ci.cancel();
            return;
        }

        if (IgnoreService.shouldHideOutgoingPacket(listener.player, packet)) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("HEAD"), cancellable = true)
    private void cointcore$filterOutgoingPacketsWithListener(Packet<?> packet, PacketSendListener listener, CallbackInfo ci) {
        if (!((Object) this instanceof ServerGamePacketListenerImpl gameListener)) {
            return;
        }

        ServerPlayer receiver = gameListener.player;
        if (JoinLeaveMessageFilter.shouldSuppressOutgoingPacket(packet)) {
            ci.cancel();
            return;
        }

        if (VanishPacketFilter.shouldCancel(server, receiver, packet)) {
            ci.cancel();
            return;
        }

        if (IgnoreService.shouldHideOutgoingPacket(receiver, packet)) {
            ci.cancel();
        }
    }
}
