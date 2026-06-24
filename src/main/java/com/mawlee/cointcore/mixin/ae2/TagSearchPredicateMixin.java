package com.mawlee.cointcore.mixin.ae2;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.menu.me.common.GridInventoryEntry;
import com.mawlee.cointcore.ae.MeFilterTags;
import com.mawlee.cointcore.ae.MeUniqueEntryRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "appeng.client.gui.me.search.TagSearchPredicate", remap = false)
public abstract class TagSearchPredicateMixin {
    @Shadow
    @Final
    private String term;

    @Inject(method = "test(Lappeng/menu/me/common/GridInventoryEntry;)Z", at = @At("RETURN"), cancellable = true, remap = false)
    private void cointcore$filterUniqueSingletons(GridInventoryEntry entry, CallbackInfoReturnable<Boolean> callback) {
        if (!Boolean.TRUE.equals(callback.getReturnValue())) {
            return;
        }
        if (!matchesCointcoreUniqueFilter()) {
            return;
        }

        AEKey key = entry.getWhat();
        if (!(key instanceof AEItemKey itemKey)) {
            callback.setReturnValue(false);
            return;
        }

        if (!itemKey.isTagged(MeFilterTags.NON_STACKABLE)) {
            return;
        }

        boolean unique = MeUniqueEntryRules.isUniqueTerminalEntry(entry.getStoredAmount(), itemKey.getReadOnlyStack());
        callback.setReturnValue(unique);
    }

    private boolean matchesCointcoreUniqueFilter() {
        String normalized = term.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains(MeFilterTags.TAG_ID)) {
            return true;
        }
        if (normalized.contains(MeFilterTags.TAG_LOCATION.getNamespace())) {
            return true;
        }
        return normalized.equals("coint") || normalized.equals("#coint");
    }
}
