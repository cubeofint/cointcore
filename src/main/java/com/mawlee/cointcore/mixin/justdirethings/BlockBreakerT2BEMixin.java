package com.mawlee.cointcore.mixin.justdirethings;

import com.direwolf20.justdirethings.common.blockentities.BlockBreakerT2BE;
import com.mawlee.cointcore.justdirethings.JustDireThingsAreaScanCache;
import com.mawlee.cointcore.justdirethings.JustDireThingsBlockValidCache;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = BlockBreakerT2BE.class, remap = false)
public abstract class BlockBreakerT2BEMixin {

    @Inject(method = "findBlocksToMine", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$useCachedAreaScan(FakePlayer fakePlayer, CallbackInfoReturnable<List<BlockPos>> cir) {
        JustDireThingsAreaScanCache.tryReturnCached((BlockBreakerT2BE) (Object) this, cir);
    }

    @Inject(method = "findBlocksToMine", at = @At("RETURN"), remap = false)
    private void cointcore$storeAreaScan(FakePlayer fakePlayer, CallbackInfoReturnable<List<BlockPos>> cir) {
        if (cir.getReturnValue() != null) {
            JustDireThingsAreaScanCache.store((BlockBreakerT2BE) (Object) this, cir.getReturnValue());
        }
    }

    @Inject(method = "isBlockValid", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$useCachedBlockValid(
            FakePlayer fakePlayer,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        JustDireThingsBlockValidCache.tryReturnCached((BlockBreakerT2BE) (Object) this, pos, cir);
    }

    @Inject(method = "isBlockValid", at = @At("RETURN"), remap = false)
    private void cointcore$storeBlockValid(
            FakePlayer fakePlayer,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        JustDireThingsBlockValidCache.store((BlockBreakerT2BE) (Object) this, pos, cir.getReturnValue());
    }
}
