package com.mawlee.cointcore.mixin.client;

import com.mawlee.cointcore.chat.client.ChatChannelClient;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chat tabs: lines of hidden channels stay in the history but are not laid out; switching tabs re-runs
 * {@code refreshTrimmedMessages}. The history keeps 1000 lines instead of 100 so tabs have something to show.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentChannelMixin {
    @Inject(method = "addMessageToDisplayQueue", at = @At("HEAD"), cancellable = true)
    private void cointcore$hideOtherChannels(GuiMessage message, CallbackInfo ci) {
        ChatChannelClient.tag(message);
        if (ChatChannelClient.isHidden(message)) {
            ci.cancel();
        }
    }

    @Inject(method = "addMessageToQueue", at = @At("HEAD"))
    private void cointcore$tagChannel(GuiMessage message, CallbackInfo ci) {
        ChatChannelClient.tag(message);
    }

    @ModifyConstant(method = "addMessageToQueue", constant = @Constant(intValue = 100), require = 0)
    private int cointcore$longerHistory(int original) {
        return 1000;
    }
}
