package com.mawlee.cointcore.mixin.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.config.RsImporterPerfConfig;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.impl.node.importer.ImporterNetworkNode;
import com.refinedmods.refinedstorage.api.network.node.importer.ImporterTransferStrategy;
import com.refinedmods.refinedstorage.api.resource.filter.Filter;
import com.refinedmods.refinedstorage.api.storage.Actor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * RS importers call {@code transfer()} on each work tick: scan adjacent slots, build
 * {@code ItemResource} (components), then SIMULATE/EXECUTE extract+insert into RootStorage.
 * When nothing moves (empty source, filter miss, full network), that work is wasted.
 * <p>
 * After an idle transfer, skip subsequent {@code doWork} for N ticks (longer when MSPT is high).
 * Active imports (transfer returned true) are not delayed.
 */
@Mixin(value = ImporterNetworkNode.class, remap = false)
public abstract class ImporterNetworkNodeIdleSkipMixin {
    @Unique
    private int cointcore$idleSkipRemaining;

    @Inject(method = "doWork", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$skipIdleWork(CallbackInfo ci) {
        if (!RsImporterPerfConfig.isEnabled()) {
            return;
        }
        if (cointcore$idleSkipRemaining > 0) {
            cointcore$idleSkipRemaining--;
            ci.cancel();
        }
    }

    @WrapOperation(
            method = "doWork",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/api/network/node/importer/ImporterTransferStrategy;transfer(Lcom/refinedmods/refinedstorage/api/resource/filter/Filter;Lcom/refinedmods/refinedstorage/api/storage/Actor;Lcom/refinedmods/refinedstorage/api/network/Network;)Z"
            ),
            remap = false
    )
    private boolean cointcore$afterTransfer(
            ImporterTransferStrategy strategy,
            Filter filter,
            Actor actor,
            Network network,
            Operation<Boolean> original
    ) {
        boolean moved = original.call(strategy, filter, actor, network);
        if (!RsImporterPerfConfig.isEnabled()) {
            return moved;
        }
        if (!moved) {
            cointcore$idleSkipRemaining = RsImporterPerfConfig.resolveIdleSkipTicksForCurrentTick();
        } else {
            cointcore$idleSkipRemaining = 0;
        }
        return moved;
    }
}
