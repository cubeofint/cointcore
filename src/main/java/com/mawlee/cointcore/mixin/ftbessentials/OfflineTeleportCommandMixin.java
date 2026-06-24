package com.mawlee.cointcore.mixin.ftbessentials;

import com.mawlee.cointcore.ftbessentials.FtbTranslatableFix;
import dev.ftb.mods.ftbessentials.commands.impl.teleporting.OfflineTeleportCommand;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = OfflineTeleportCommand.class, remap = false)
public abstract class OfflineTeleportCommandMixin {
    @Redirect(
            method = "lambda$tpOffline$6(Ljava/util/UUID;Ljava/lang/String;Lnet/minecraft/commands/CommandSourceStack;)Lnet/minecraft/network/chat/Component;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            require = 1
    )
    private static MutableComponent cointcore$safeOfflineTeleportMessage(String key, Object[] args) {
        return FtbTranslatableFix.translatable(key, args);
    }
}
