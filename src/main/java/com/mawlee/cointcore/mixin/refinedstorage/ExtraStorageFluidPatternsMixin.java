package com.mawlee.cointcore.mixin.refinedstorage;

import com.refinedmods.refinedstorage.api.autocrafting.Pattern;
import com.refinedmods.refinedstorage.api.autocrafting.PatternLayout;
import com.refinedmods.refinedstorage.api.network.impl.node.patternprovider.PatternProviderNetworkNode;
import com.refinedmods.refinedstorage.common.support.FilteredContainer;
import com.ultramega.refinedfluidsubstitution.common.fluidsubstitutionpattern.FluidSubstitutionPatternItem;
import com.ultramega.refinedfluidsubstitution.common.fluidsubstitutionpattern.FluidSubstitutionPatternResolver;
import com.ultramega.refinedfluidsubstitution.common.util.MainNetworkNodeAccessor;
import com.ultramega.refinedfluidsubstitution.common.util.PatternProviderNetworkNodeExtension;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/** ExtraStorage has its own block entity, so the addon's native RS hooks never run on it. */
@Mixin(targets = "edivad.extrastorage.autocrafting.advancedautocrafter.AdvancedAutocrafterBlockEntity", remap = false)
public abstract class ExtraStorageFluidPatternsMixin {
    @Shadow public abstract FilteredContainer getPatternContainer();

    @Unique private boolean cointcore$fluidPatternsDirty;

    @Inject(method = "patternChanged", at = @At("RETURN"), require = 1)
    private void cointcore$queueFluidPatterns(CallbackInfo ci) {
        cointcore$fluidPatternsDirty = true;
    }

    @Inject(method = "doWork", at = @At("HEAD"), require = 1)
    private void cointcore$refreshFluidPatterns(CallbackInfo ci) {
        // Defer changes until outside the item transfer transaction, just as the addon does.
        if (!cointcore$fluidPatternsDirty) return;
        var level = ((BlockEntity) (Object) this).getLevel();
        if (level == null || level.isClientSide()) return;
        cointcore$fluidPatternsDirty = false;
        var node = (PatternProviderNetworkNode) ((MainNetworkNodeAccessor) this).rfs$getMainNetworkNode();
        var extension = (PatternProviderNetworkNodeExtension) node;
        var inventory = getPatternContainer();
        int size = inventory.getContainerSize();
        Map<PatternLayout, Pattern> helpers = new LinkedHashMap<>();
        for (int slot = 0; slot < size; slot++) {
            var stack = inventory.getItem(slot);
            if (!(stack.getItem() instanceof FluidSubstitutionPatternItem)) continue;
            var resolved = FluidSubstitutionPatternResolver.resolve(stack, level);
            if (resolved.isEmpty()) continue;
            node.setPattern(slot, resolved.get().pattern());
            for (var helper : resolved.get().helperPatterns()) {
                helpers.putIfAbsent(helper.layout(), helper);
            }
        }
        // One auxiliary pattern per bucket slot; keep physical pattern slots untouched.
        extension.rfs$ensurePatternCapacity(size + helpers.size());
        int slot = size;
        for (var helper : helpers.values()) node.setPattern(slot++, helper);
        while (slot < extension.rfs$getPatternCapacity()) node.setPattern(slot++, null);
    }
}
