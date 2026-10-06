package com.mawlee.cointcore.mixin.ae2;

import appeng.api.stacks.AEItemKey;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mawlee.cointcore.config.Ae2PerfConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * AE2 already caches {@code damage} / {@code maxStackSize} on {@link AEItemKey}, but
 * {@code getFuzzySearchMaxValue()} re-reads {@code ItemStack.getMaxDamage()} every time.
 * {@link appeng.api.stacks.KeyCounter} calls that on every {@code add}/{@code set} to choose
 * Fuzzy vs Unordered maps — which hammers DataComponentMap ({@code Reference2ObjectArrayMap.get})
 * and owo's DerivedComponentMap during {@code StorageService.updateCachedStacks}.
 * <p>
 * Cache once per key instance (stack is an immutable copy owned by the key).
 */
@Mixin(value = AEItemKey.class, remap = false)
public abstract class AEItemKeyFuzzyMaxCacheMixin {
    @Unique
    private static final int COINTCORE$UNSET = Integer.MIN_VALUE;

    @Unique
    private int cointcore$cachedFuzzyMax = COINTCORE$UNSET;

    @WrapMethod(method = "getFuzzySearchMaxValue", remap = false)
    private int cointcore$cacheFuzzySearchMaxValue(Operation<Integer> original) {
        if (!Ae2PerfConfig.isCacheFuzzySearchMaxValue()) {
            return original.call();
        }
        if (cointcore$cachedFuzzyMax != COINTCORE$UNSET) {
            return cointcore$cachedFuzzyMax;
        }
        int value = original.call();
        cointcore$cachedFuzzyMax = value;
        return value;
    }
}
