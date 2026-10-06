package com.mawlee.cointcore.mixin.tab;

import com.mawlee.cointcore.vanish.VanishManager;
import me.neznamy.tab.platforms.neoforge.NeoForgeTabPlayer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NeoForgeTabPlayer.class, remap = false)
public abstract class NeoForgeTabPlayerVanishedMixin {
    @Shadow
    public abstract ServerPlayer getPlayer();

    @Inject(method = "isVanished0", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$markCointCoreVanish(CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer player = getPlayer();
        if (player != null && VanishManager.isVanished(player)) {
            cir.setReturnValue(true);
        }
    }
}
