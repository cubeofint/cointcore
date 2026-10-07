package com.mawlee.cointcore.invsee;

/**
 * Classification of right-click nested item contents. Pure flags, no ItemStack.
 */
public enum InvSeeNestedKind {
    NONE,
    CONTAINER,
    BUNDLE,
    BACKPACK;

    public static InvSeeNestedKind classify(
            boolean backpackItem,
            boolean bundleContents,
            boolean containerContents,
            boolean shulkerLikeId
    ) {
        if (backpackItem) {
            return BACKPACK;
        }
        if (bundleContents) {
            return BUNDLE;
        }
        if (containerContents || shulkerLikeId) {
            return CONTAINER;
        }
        return NONE;
    }

    public boolean opensMenu() {
        return this != NONE;
    }
}
