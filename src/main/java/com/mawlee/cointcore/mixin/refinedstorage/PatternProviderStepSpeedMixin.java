package com.mawlee.cointcore.mixin.refinedstorage;

import com.mawlee.cointcore.config.RsAutocraftingConfig;
import com.refinedmods.refinedstorage.api.autocrafting.Pattern;
import com.refinedmods.refinedstorage.api.autocrafting.task.StepBehavior;
import com.refinedmods.refinedstorage.api.network.impl.node.patternprovider.PatternProviderNetworkNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * A task steps every pattern with the step behavior of the autocrafter that owns the task. For a sub-pattern that
 * lives in another autocrafter it asks that provider node, which only has {@link StepBehavior}'s defaults (always,
 * one step), so upgrades, Cable Tiers tiers and multiblock CPUs of that autocrafter do nothing. Answer with the
 * behavior its block entity installed instead.
 */
@Mixin(value = PatternProviderNetworkNode.class, remap = false)
public abstract class PatternProviderStepSpeedMixin implements StepBehavior {
    @Unique
    private static final ThreadLocal<int[]> COINTCORE$DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    @Shadow(remap = false)
    private StepBehavior stepBehavior;

    @Override
    public boolean canStep(Pattern pattern) {
        StepBehavior behavior = cointcore$delegate();
        if (behavior == null) return true;
        int[] depth = COINTCORE$DEPTH.get();
        depth[0]++;
        try {
            return behavior.canStep(pattern);
        } finally {
            depth[0]--;
        }
    }

    @Override
    public int getSteps(Pattern pattern) {
        StepBehavior behavior = cointcore$delegate();
        if (behavior == null) return 1;
        int[] depth = COINTCORE$DEPTH.get();
        depth[0]++;
        try {
            return behavior.getSteps(pattern);
        } finally {
            depth[0]--;
        }
    }

    /** Null means stock behavior; the depth guard stops any provider cycle between block entities. */
    @Unique
    private StepBehavior cointcore$delegate() {
        if (!RsAutocraftingConfig.isProviderSpeed() || stepBehavior == null || stepBehavior == StepBehavior.DEFAULT) return null;
        if (stepBehavior == (Object) this) return null;
        return COINTCORE$DEPTH.get()[0] >= 4 ? null : stepBehavior;
    }
}
