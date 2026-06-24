package com.mawlee.cointcore.mixin.create;

import com.mawlee.cointcore.claim.ClaimGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(value = com.simibubi.create.foundation.utility.BlockHelper.class, remap = false)
public abstract class BlockHelperDestroyMixin {
    @Inject(
            method = "destroyBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;FLjava/util/function/Consumer;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void cointcore$guardDestroyBlock(
            Level level,
            BlockPos pos,
            float hardness,
            Consumer<ItemStack> consumer,
            CallbackInfo ci
    ) {
        if (!ClaimGuard.canEditAtCurrentActor(level, pos, Optional.empty())) {
            ci.cancel();
        }
    }
}
