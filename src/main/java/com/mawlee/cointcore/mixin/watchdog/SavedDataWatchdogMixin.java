package com.mawlee.cointcore.mixin.watchdog;

import com.mawlee.cointcore.watchdog.probes.ProbeHooks;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SavedData.class)
public abstract class SavedDataWatchdogMixin {
    @Inject(
            method = "save(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("HEAD"),
            require = 0
    )
    private void cointcore$watchdogSavedDataStart(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfoReturnable<CompoundTag> cir
    ) {
        ProbeHooks.onSavedDataStart();
    }

    @Inject(
            method = "save(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/CompoundTag;",
            at = @At("RETURN"),
            require = 0
    )
    private void cointcore$watchdogSavedDataEnd(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfoReturnable<CompoundTag> cir
    ) {
        ProbeHooks.onSavedDataEnd(this.getClass().getName());
    }
}
