package com.mawlee.cointcore.mixin.ftbessentials;

import com.mawlee.cointcore.ftbessentials.FtbTranslatableFix;
import dev.ftb.mods.ftbessentials.commands.impl.kit.KitCommand;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = KitCommand.class, remap = false)
public abstract class KitCommandCooldownResetMixin {
    @Redirect(
            method = "lambda$resetCooldowns$58(Ljava/lang/String;Ljava/util/UUID;)Lnet/minecraft/network/chat/Component;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            require = 1
    )
    private static MutableComponent cointcore$safeCooldownResetMessage(String key, Object[] args) {
        return FtbTranslatableFix.translatable(key, args);
    }

    @Redirect(
            method = "lambda$static$2(Ljava/lang/Object;)Lcom/mojang/brigadier/Message;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            require = 0
    )
    private static MutableComponent cointcore$safeUnknownPlayerIdMessage(String key, Object[] args) {
        return FtbTranslatableFix.translatable(key, args);
    }
}
