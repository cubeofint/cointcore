package com.mawlee.cointcore.mixin.ftbessentials;

import dev.ftb.mods.ftbessentials.commands.impl.kit.GiveMeKitCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = GiveMeKitCommand.class, remap = false)
public abstract class GiveMeKitCommandAliasMixin {
    @ModifyConstant(method = "register", constant = @Constant(stringValue = "give_me_kit"))
    private static String cointcore$useKitCommand(String original) {
        return "kit";
    }
}
