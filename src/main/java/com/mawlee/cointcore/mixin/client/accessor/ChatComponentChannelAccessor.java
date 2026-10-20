package com.mawlee.cointcore.mixin.client.accessor;

import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChatComponent.class)
public interface ChatComponentChannelAccessor {
    @Invoker("refreshTrimmedMessages")
    void cointcore$refreshTrimmedMessages();
}
