package com.mawlee.cointcore.mixin.ae2;

import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.helpers.MultiCraftingTracker;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mawlee.cointcore.ae.CraftingRetryGate;
import com.mawlee.cointcore.config.Ae2PerfConfig;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.concurrent.Future;

/** Automatic buses/interfaces retry rejected plans; pending calculations and accepted jobs stay native. */
@Mixin(value = MultiCraftingTracker.class, remap = false)
public abstract class MultiCraftingRetryMixin {
    @Shadow private Future<ICraftingPlan>[] jobs;
    @Unique private CraftingRetryGate<AEKey> cointcore$retries;

    @Inject(method = "cancel", at = @At("HEAD"), require = 1)
    private void cointcore$clearRetryState(CallbackInfo callback) {
        cointcore$retries = null;
    }

    @Inject(method = "handleCrafting", at = @At("HEAD"), cancellable = true, require = 1)
    private void cointcore$delayFailedRequest(int slot, AEKey key, long amount, Level level,
            ICraftingService service, IActionSource source, CallbackInfoReturnable<Boolean> callback) {
        if (cointcore$retries != null && (jobs == null || jobs[slot] == null)
                && cointcore$retries.shouldDelay(slot, key, amount, level, level.getGameTime(),
                        Ae2PerfConfig.getAutomaticCraftingRetryTicks())) callback.setReturnValue(false);
    }

    @ModifyExpressionValue(method = "handleCrafting", at = @At(value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingSubmitResult;successful()Z"), require = 1)
    private boolean cointcore$rememberRejectedPlan(boolean successful, int slot, AEKey key, long amount,
            Level level, ICraftingService service, IActionSource source) {
        if (!successful && Ae2PerfConfig.getAutomaticCraftingRetryTicks() > 1) {
            if (cointcore$retries == null) cointcore$retries = new CraftingRetryGate<>();
            cointcore$retries.failed(slot, key, amount, level, level.getGameTime());
        }
        return successful;
    }
}
