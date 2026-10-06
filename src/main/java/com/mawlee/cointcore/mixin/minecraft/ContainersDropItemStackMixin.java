package com.mawlee.cointcore.mixin.minecraft;

import com.mawlee.cointcore.item.MassDropGuard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Budget mass container drops (see {@link MassDropGuard}).
 */
@Mixin(Containers.class)
public abstract class ContainersDropItemStackMixin {
    @Inject(
            method = "dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void cointcore$budgetMassDrop(Level level, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel && MassDropGuard.handle(serverLevel, x, y, z, stack)) {
            ci.cancel();
        }
    }
}
