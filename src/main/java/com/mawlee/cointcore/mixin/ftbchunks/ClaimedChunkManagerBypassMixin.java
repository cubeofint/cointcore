package com.mawlee.cointcore.mixin.ftbchunks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl.class, remap = false)
public abstract class ClaimedChunkManagerBypassMixin {
    @Shadow
    public abstract boolean getBypassProtection(UUID player);

    @Inject(method = "shouldPreventInteraction", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$honorBypassProtection(
            Entity actor,
            InteractionHand hand,
            BlockPos pos,
            dev.ftb.mods.ftbchunks.api.Protection protection,
            Entity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (actor instanceof ServerPlayer player && getBypassProtection(player.getUUID())) {
            cir.setReturnValue(false);
        }
    }
}
