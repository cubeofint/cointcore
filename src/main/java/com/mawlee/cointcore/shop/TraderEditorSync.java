package com.mawlee.cointcore.shop;

/**
 * When to push server-synced lot values into the manage-screen editor.
 * Background catalog/restock refreshes must not clobber in-progress typing.
 */
public final class TraderEditorSync {
    private TraderEditorSync() {
    }

    public static boolean showActionStatus(boolean userAction) {
        return userAction;
    }

    public static boolean applyServerValuesToEditor(boolean userAction, boolean lastActionOk, boolean editorFocused) {
        return userAction && lastActionOk && !editorFocused;
    }
}
