package com.mawlee.cointcore.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.chat.ChatChannel;
import com.mawlee.cointcore.chat.ChatChannelService;
import com.mawlee.cointcore.chat.LocalChatService;
import com.mawlee.cointcore.config.ChatChannelsConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Advancement / challenge announcements go to the local channel only (players within the local radius).
 * {@code discord_chat_mod} relays them to Discord from its {@code @ModifyArg} on the same call, which still runs.
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsLocalMixin {
    @Shadow
    private ServerPlayer player;

    @WrapOperation(
            method = "lambda$award$2",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
            ),
            require = 0
    )
    private void cointcore$announceLocally(PlayerList playerList, Component message, boolean overlay, Operation<Void> original) {
        ChatChannelsConfig.Settings settings = ChatChannelsConfig.get();
        if (!settings.enabled() || !settings.advancementsLocal() || player == null) {
            original.call(playerList, message, overlay);
            return;
        }
        for (ServerPlayer target : playerList.getPlayers()) {
            if (LocalChatService.isWithinLocalRange(player, target)) {
                ChatChannelService.deliver(target, ChatChannel.LOCAL, message);
            }
        }
    }
}
