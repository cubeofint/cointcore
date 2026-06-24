package com.mawlee.cointcore.mixin.ftbessentials;

import dev.ftb.mods.ftbessentials.commands.impl.kit.KitCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = KitCommand.class, remap = false)
public abstract class KitCommandAdminRenameMixin {
    @ModifyConstant(method = "register", constant = @Constant(stringValue = "kit"))
    private static String cointcore$useAdminKitCommand(String original) {
        return "ftb_kit";
    }

    @ModifyArg(
            method = "listKits",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/ClickEvent;<init>(Lnet/minecraft/network/chat/ClickEvent$Action;Ljava/lang/String;)V"
            ),
            index = 1,
            remap = true,
            require = 0
    )
    private static String cointcore$fixKitListClickCommand(String command) {
        if (command.startsWith("/kit show ")) {
            return "/ftb_kit show " + command.substring(10);
        }
        if (command.startsWith("kit show ")) {
            return "ftb_kit show " + command.substring(9);
        }
        return command;
    }
}
