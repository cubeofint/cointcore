package com.mawlee.cointcore.mixin.discord;

import com.denisnumb.discord_chat_mod.MinecraftUtils;
import com.mawlee.cointcore.vanish.DiscordVanishBridge;
import com.mawlee.cointcore.vanish.VanishManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MinecraftUtils.class, remap = false)
public abstract class MinecraftUtilsMixin {
    @Inject(method = "getServerPlayerCount", at = @At("RETURN"), cancellable = true, remap = false)
    private static void cointcore$filterVanishedCount(
            MinecraftServer server,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (server == null || VanishManager.vanishedPlayers().isEmpty()) {
            return;
        }

        cir.setReturnValue(DiscordVanishBridge.visiblePlayerCount(server));
    }

    @Inject(method = "getServerPlayerNames", at = @At("RETURN"), cancellable = true, remap = false)
    private static void cointcore$filterVanishedNames(
            MinecraftServer server,
            CallbackInfoReturnable<String[]> cir
    ) {
        if (server == null || VanishManager.vanishedPlayers().isEmpty()) {
            return;
        }

        cir.setReturnValue(DiscordVanishBridge.visiblePlayerNames(server));
    }
}
