package com.mawlee.cointcore.mixin.watchdog;

import com.mawlee.cointcore.watchdog.probes.ProbeHooks;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

/**
 * Times per-SavedData disk writes. Must not inject into
 * {@code save(CompoundTag, Provider)} — that method is abstract in 1.21.1 and Mixin
 * NPEs in {@code InsnList.indexOf} while preparing {@code @Inject} (require=0 does not help).
 */
@Mixin(SavedData.class)
public abstract class SavedDataWatchdogMixin {
    @Inject(
            method = "save(Ljava/io/File;Lnet/minecraft/core/HolderLookup$Provider;)V",
            at = @At("HEAD"),
            require = 0
    )
    private void cointcore$watchdogSavedDataStart(
            File file,
            HolderLookup.Provider registries,
            CallbackInfo ci
    ) {
        ProbeHooks.onSavedDataStart();
    }

    @Inject(
            method = "save(Ljava/io/File;Lnet/minecraft/core/HolderLookup$Provider;)V",
            at = @At("RETURN"),
            require = 0
    )
    private void cointcore$watchdogSavedDataEnd(
            File file,
            HolderLookup.Provider registries,
            CallbackInfo ci
    ) {
        ProbeHooks.onSavedDataEnd(this.getClass().getName());
    }
}
