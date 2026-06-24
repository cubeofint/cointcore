package com.mawlee.cointcore.mixin;

import net.minecraft.server.commands.MsgCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MsgCommand.class)
public abstract class MsgCommandMixin {
    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void cointcore$skipVanillaPrivateMessageCommands(
            com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}
