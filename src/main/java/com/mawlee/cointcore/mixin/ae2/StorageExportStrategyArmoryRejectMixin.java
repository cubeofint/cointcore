package com.mawlee.cointcore.mixin.ae2;

import appeng.api.behaviors.StackTransferContext;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.parts.automation.StorageExportStrategy;
import com.mawlee.cointcore.config.PipezArmoryFastRejectConfig;
import com.mawlee.cointcore.pipez.PipezArmoryFastReject;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AE2 export buses (incl. Extended AE tag/mod export buses) first extract from the network — scanning every slot of
 * storage-bus-attached chests — and only then offer the item to the target. Facing a Functional Storage Armory
 * Cabinet, almost every offer is rejected after {@code ItemHandlerHelper.insertItem} walked 4096 slots (~19% of the
 * server thread in one profile). Ask the cabinet once up front and skip the transfer for items it will never accept.
 *
 * <p>Shares {@code pipez_armory_fast_reject.enabled} in {@code lag-fixes.json}. Verified against AE2 19.2.18.
 */
@Mixin(value = StorageExportStrategy.class, remap = false)
public abstract class StorageExportStrategyArmoryRejectMixin<T, S> {
    @Shadow
    @Final
    private BlockCapabilityCache<T, Direction> cache;

    @Inject(method = "transfer", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$skipArmoryRejects(StackTransferContext context, AEKey what, long amount,
                                            CallbackInfoReturnable<Long> cir) {
        if (!PipezArmoryFastRejectConfig.isEnabled() || !(what instanceof AEItemKey itemKey)) {
            return;
        }
        Object target = cache.getCapability();
        if (target instanceof IItemHandler handler
                && PipezArmoryFastReject.isArmoryCabinet(handler)
                && !handler.isItemValid(0, itemKey.toStack())) {
            PipezArmoryFastReject.recordSkippedInsert();
            cir.setReturnValue(0L);
        }
    }
}
