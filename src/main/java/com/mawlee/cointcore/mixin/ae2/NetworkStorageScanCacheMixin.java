package com.mawlee.cointcore.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.me.storage.NetworkStorage;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mawlee.cointcore.ae2.NetworkInventoryScanCache;
import com.mawlee.cointcore.config.Ae2PerfConfig;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NetworkStorage.class, remap = false)
public abstract class NetworkStorageScanCacheMixin {
    @Shadow private boolean mountsInUse;
    @Unique private final NetworkInventoryScanCache cointcore$inventoryScanCache = new NetworkInventoryScanCache();

    @WrapMethod(method = "getAvailableStacks")
    private void cointcore$reuseInventoryScan(KeyCounter out, Operation<Void> original) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) {
            original.call(out);
            return;
        }
        if (!Ae2PerfConfig.isCacheNetworkInventoryPerTick()) {
            cointcore$inventoryScanCache.clear();
            original.call(out);
            return;
        }
        cointcore$inventoryScanCache.read(server, server.getTickCount(), mountsInUse, out, values -> original.call(values));
    }

    @Inject(method = {"insert", "extract"}, at = @At("HEAD"), require = 2)
    private void cointcore$beforeInventoryChange(AEKey key, long amount, Actionable mode, IActionSource source,
                                                CallbackInfoReturnable<Long> ci) {
        if (mode == Actionable.MODULATE) {
            NetworkInventoryScanCache.invalidate();
        }
    }

    @Inject(method = {"insert", "extract"}, at = @At("RETURN"), require = 2)
    private void cointcore$afterInventoryChange(AEKey key, long amount, Actionable mode, IActionSource source,
                                               CallbackInfoReturnable<Long> ci) {
        if (mode == Actionable.MODULATE && ci.getReturnValue() > 0) {
            NetworkInventoryScanCache.invalidate();
        }
    }

    @Inject(method = {"mount", "unmount"}, at = @At("HEAD"), require = 2)
    private void cointcore$mountsChanged(CallbackInfo ci) {
        NetworkInventoryScanCache.invalidate();
    }
}
