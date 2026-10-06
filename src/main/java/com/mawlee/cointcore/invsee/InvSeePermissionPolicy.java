package com.mawlee.cointcore.invsee;

/**
 * Pure permission rules for InvSee. Callers supply already-resolved node states
 * so this can be unit-tested without Minecraft or LuckPerms.
 */
public final class InvSeePermissionPolicy {
    private InvSeePermissionPolicy() {
    }

    /**
     * Section access: an explicit {@code false} on the granular node wins;
     * {@code unset} falls back to the legacy blanket node.
     */
    public static boolean allows(InvSeeTriState specific, InvSeeTriState legacyFallback) {
        if (specific == InvSeeTriState.FALSE) {
            return false;
        }
        if (specific == InvSeeTriState.TRUE) {
            return true;
        }
        return legacyFallback == InvSeeTriState.TRUE;
    }

    public static boolean canUseCommand(InvSeeTriState legacyView, InvSeeTriState[] sectionViews) {
        if (legacyView == InvSeeTriState.TRUE) {
            return true;
        }
        if (sectionViews == null) {
            return false;
        }
        for (InvSeeTriState sectionView : sectionViews) {
            if (sectionView == InvSeeTriState.TRUE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Protected inventories: {@code exempt.bypass} always wins. Otherwise a target
     * with {@code exempt} can be inspected only by a strictly higher weight.
     */
    public static boolean canInspect(
            InvSeeTriState viewerBypass,
            InvSeeTriState targetExempt,
            int viewerWeight,
            int targetWeight
    ) {
        if (viewerBypass == InvSeeTriState.TRUE) {
            return true;
        }
        if (targetExempt != InvSeeTriState.TRUE) {
            return true;
        }
        return viewerWeight > targetWeight;
    }
}
