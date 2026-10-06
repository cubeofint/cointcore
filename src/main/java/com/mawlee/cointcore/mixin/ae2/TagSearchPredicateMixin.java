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

import java.util.Locale;

@Mixin(targets = "appeng.client.gui.me.search.TagSearchPredicate", remap = false)
public abstract class TagSearchPredicateMixin {
    @Shadow
    @Final
    private String term;

    @Inject(method = "test(Lappeng/menu/me/common/GridInventoryEntry;)Z", at = @At("RETURN"), cancellable = true, remap = false)
    private void cointcore$filterNonStackableEntries(GridInventoryEntry entry, CallbackInfoReturnable<Boolean> callback) {
        boolean mergedMode = cointcore$isMergedTerm();
        boolean uniqueMode = !mergedMode && cointcore$isUniqueTerm();
        if (!mergedMode && !uniqueMode) {
            return;
        }

        AEKey key = entry.getWhat();
        if (!(key instanceof AEItemKey itemKey)) {
            callback.setReturnValue(false);
            return;
        }

        if (mergedMode) {
            // Short aliases like #nsm are not substring-matches of the tag path, so AE2 may
            // return false — we still own the decision for merged mode.
            boolean inTag = itemKey.isTagged(MeFilterTags.NON_STACKABLE_MERGED)
                    || itemKey.isTagged(MeFilterTags.NON_STACKABLE);
            callback.setReturnValue(
                    inTag && MeUniqueEntryRules.isMergedTerminalEntry(entry.getStoredAmount(), itemKey.getReadOnlyStack())
            );
            return;
        }

        // Unique mode still requires AE2 to have matched a cointcore tag first, unless the
        // entry is explicitly in our non_stackable tag (covers #coint / namespace shortcuts).
        if (!Boolean.TRUE.equals(callback.getReturnValue()) && !itemKey.isTagged(MeFilterTags.NON_STACKABLE)) {
            return;
        }
        if (!itemKey.isTagged(MeFilterTags.NON_STACKABLE)) {
            callback.setReturnValue(false);
            return;
        }

        callback.setReturnValue(
                MeUniqueEntryRules.isUniqueTerminalEntry(entry.getStoredAmount(), itemKey.getReadOnlyStack())
        );
    }

    private boolean cointcore$isMergedTerm() {
        String normalized = cointcore$normalizedTerm();
        if (normalized.contains(MeFilterTags.MERGED_TAG_ID)
                || normalized.contains(MeFilterTags.MERGED_TAG_ID_LEGACY)) {
            return true;
        }
        return normalized.equals("nsm")
                || normalized.equals("#nsm")
                || normalized.equals("heavy")
                || normalized.equals("#heavy");
    }

    private boolean cointcore$isUniqueTerm() {
        String normalized = cointcore$normalizedTerm();
        if (normalized.contains(MeFilterTags.TAG_ID)) {
            return true;
        }
        if (normalized.contains(MeFilterTags.TAG_LOCATION.getNamespace())) {
            return true;
        }
        return normalized.equals("coint") || normalized.equals("#coint");
    }

    private String cointcore$normalizedTerm() {
        return term.toLowerCase(Locale.ROOT);
    }
}
