package com.mawlee.cointcore.mixin.tab;

import com.mawlee.cointcore.vanish.VanishManager;
import me.neznamy.tab.shared.platform.TabPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = TabPlayer.class, remap = false)
public abstract class TabPlayerCanSeeMixin {
    @Shadow
    public abstract UUID getUniqueId();

    @Inject(method = "canSee", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$hideVanishedPlayers(TabPlayer other, CallbackInfoReturnable<Boolean> cir) {
        if (other == null) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        ServerPlayer viewer = server.getPlayerList().getPlayer(getUniqueId());
        ServerPlayer target = server.getPlayerList().getPlayer(other.getUniqueId());
        if (viewer == null || target == null || !VanishManager.isVanished(target)) {
            return;
        }

        cir.setReturnValue(!VanishManager.shouldHideFrom(target, viewer));
    }
}
