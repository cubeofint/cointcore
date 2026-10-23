package com.mawlee.cointcore.mixin.ae2;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.mawlee.cointcore.ae.SophisticatedFastExtract;
import com.mawlee.cointcore.config.Ae2PerfConfig;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A storage bus extracting from a Sophisticated Storage chest walked every slot of it, comparing each stack with the
 * requested key (~8% of the server thread with big chests on buses). Sophisticated keeps an index of which slots hold
 * which stack; extract through it instead. Output-filter upgrades still apply (the tracked handler checks them).
 *
 * <p>Verified against AE2 19.2.18 ({@code ExternalStorageFacade$ItemHandlerFacade#extractExternal}).
 */
@Mixin(targets = "appeng.me.storage.ExternalStorageFacade$ItemHandlerFacade", remap = false)
public abstract class ItemHandlerFacadeSophisticatedMixin {
    @Shadow
    @Final
    private IItemHandler handler;

    @Inject(method = "extractExternal", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$extractViaSlotIndex(AEKey what, int amount, Actionable mode, CallbackInfoReturnable<Integer> cir) {
        if (amount <= 0 || !(what instanceof AEItemKey itemKey) || !Ae2PerfConfig.isSophisticatedFastExtract()) {
            return;
        }
        Object tracked = SophisticatedFastExtract.resolve(handler);
        if (tracked == null) {
            return;
        }
        int extracted = SophisticatedFastExtract.extract(tracked, itemKey.toStack(), amount, mode == Actionable.SIMULATE);
        if (extracted >= 0) {
            cir.setReturnValue(extracted);
        }
    }
}
