package com.mawlee.cointcore.mixin.justdirethings;

import com.direwolf20.justdirethings.common.blockentities.SensorT2BE;
import com.mawlee.cointcore.justdirethings.JustDireThingsAreaScanCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = SensorT2BE.class, remap = false)
public abstract class SensorT2BEMixin {

    @Inject(method = "findPositions", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$useCachedAreaScan(CallbackInfoReturnable<List<BlockPos>> cir) {
        JustDireThingsAreaScanCache.tryReturnCached((BlockEntity) (Object) this, cir);
    }

    @Inject(method = "findPositions", at = @At("RETURN"), remap = false)
    private void cointcore$storeAreaScan(CallbackInfoReturnable<List<BlockPos>> cir) {
        if (cir.getReturnValue() != null) {
            JustDireThingsAreaScanCache.store((BlockEntity) (Object) this, cir.getReturnValue());
        }
    }
}
