package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.chatspy.ChatSpyTracker;
import com.mawlee.cointcore.ignore.IgnoreService;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "sendPlayerChatMessage", at = @At("HEAD"), cancellable = true)
    private void cointcore$filterIgnoredPlayerChat(PlayerChatMessage message, ChatType.Bound bound, CallbackInfo ci) {
        if (IgnoreService.shouldHidePlayerChatMessage(player, message)) {
            ci.cancel();
            return;
        }

        ChatSpyTracker.recordPlayerChat(player, message, bound);
    }

    @Inject(method = "sendDisguisedChatMessage", at = @At("HEAD"))
    private void cointcore$trackDisguisedChat(Component message, ChatType.Bound boundType, CallbackInfo ci) {
        ChatSpyTracker.recordSystemChat(player, message);
    }
}
