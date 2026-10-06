package com.mawlee.cointcore.mixin.discord;

import com.denisnumb.discord_chat_mod.MinecraftEvents;
import com.mawlee.cointcore.vanish.VanishManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = MinecraftEvents.class, remap = false)
public abstract class MinecraftEventsMixin {
    @Inject(method = "handleJoinLeave", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$suppressVanishedJoinLeave(
            Player player,
            boolean joined,
            CallbackInfoReturnable<Optional<Component>> cir
    ) {
        if (player instanceof ServerPlayer serverPlayer && VanishManager.isVanished(serverPlayer)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
