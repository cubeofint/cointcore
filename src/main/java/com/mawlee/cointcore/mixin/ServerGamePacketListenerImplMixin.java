package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.afk.AfkActivityStrength;
import com.mawlee.cointcore.afk.AfkService;
import com.mawlee.cointcore.chat.ChatLinkFormatter;
import com.mawlee.cointcore.chatspy.ChatSpyTracker;
import com.mawlee.cointcore.ignore.IgnoreService;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Shadow
    public abstract void sendDisguisedChatMessage(Component message, ChatType.Bound boundType);

    @Inject(method = "sendPlayerChatMessage", at = @At("HEAD"), cancellable = true)
    private void cointcore$filterIgnoredPlayerChat(PlayerChatMessage message, ChatType.Bound bound, CallbackInfo ci) {
        if (IgnoreService.shouldHidePlayerChatMessage(player, message)) {
            ci.cancel();
            return;
        }

        Component content = message.decoratedContent();
        if (ChatLinkFormatter.needsLinkify(content)) {
            // Signed chat packets cannot carry new ClickEvents; resend as disguised chat.
            sendDisguisedChatMessage(ChatLinkFormatter.linkify(content), bound);
            ci.cancel();
            return;
        }

        ChatSpyTracker.recordPlayerChat(player, message, bound);
    }

    @ModifyVariable(
            method = "sendDisguisedChatMessage",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Component cointcore$linkifyDisguisedChat(Component message) {
        return ChatLinkFormatter.linkify(message);
    }

    @Inject(method = "sendDisguisedChatMessage", at = @At("HEAD"))
    private void cointcore$trackDisguisedChat(Component message, ChatType.Bound boundType, CallbackInfo ci) {
        ChatSpyTracker.recordSystemChat(player, message);
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void cointcore$afkMoveLook(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        AfkService.onMoveLook(player, packet);
    }

    @Inject(method = "handlePlayerInput", at = @At("HEAD"))
    private void cointcore$afkPlayerInput(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
        AfkService.onPlayerInput(player, packet);
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"))
    private void cointcore$afkPlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        AfkService.touch(player, AfkService.ACTIVITY_PLAYER_ACTION, AfkActivityStrength.STRONG);
    }

    @Inject(method = "handleUseItem", at = @At("HEAD"))
    private void cointcore$afkUseItem(ServerboundUseItemPacket packet, CallbackInfo ci) {
        AfkService.touch(player, AfkService.ACTIVITY_USE_ITEM, AfkActivityStrength.STRONG);
    }

    @Inject(method = "handleUseItemOn", at = @At("HEAD"))
    private void cointcore$afkUseItemOn(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        AfkService.touch(player, AfkService.ACTIVITY_USE_ITEM_ON, AfkActivityStrength.STRONG);
    }

    @Inject(method = "handleInteract", at = @At("HEAD"))
    private void cointcore$afkInteract(ServerboundInteractPacket packet, CallbackInfo ci) {
        AfkService.touch(player, AfkService.ACTIVITY_INTERACT, AfkActivityStrength.STRONG);
    }

    @Inject(method = "handleAnimate", at = @At("HEAD"))
    private void cointcore$afkAnimate(ServerboundSwingPacket packet, CallbackInfo ci) {
        AfkService.touch(player, AfkService.ACTIVITY_SWING, AfkActivityStrength.WEAK);
    }

    @Inject(method = "handleSetCarriedItem", at = @At("HEAD"))
    private void cointcore$afkSetCarriedItem(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
        AfkService.touch(player, AfkService.ACTIVITY_HOTBAR, AfkActivityStrength.WEAK);
    }

    @Inject(method = "handleContainerClick", at = @At("HEAD"))
    private void cointcore$afkContainerClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        AfkService.touch(player, AfkService.ACTIVITY_CONTAINER_CLICK, AfkActivityStrength.WEAK);
    }

    @Inject(method = "handleContainerClose", at = @At("HEAD"))
    private void cointcore$afkContainerClose(ServerboundContainerClosePacket packet, CallbackInfo ci) {
        AfkService.touch(player, AfkService.ACTIVITY_CONTAINER_CLOSE, AfkActivityStrength.WEAK);
    }
}
