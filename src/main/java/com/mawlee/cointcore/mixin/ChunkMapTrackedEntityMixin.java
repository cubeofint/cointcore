package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.vanish.VanishVisibility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class ChunkMapTrackedEntityMixin {
    @Shadow
    @Final
    Entity entity;

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void cointcore$preventVanishedTracking(ServerPlayer viewer, CallbackInfo ci) {
        if (entity instanceof Player player && VanishVisibility.isHiddenFrom(viewer, player)) {
            ci.cancel();
        }
    }
}
