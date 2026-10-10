package com.mawlee.cointcore.ui;

/**
 * Visibility for list pagers: hide the whole chrome on a single page,
 * hide {@code <} on the first page and {@code >} on the last.
 */
public final class PagerChrome {
    private PagerChrome() {
    }

    public static boolean showLabel(int maxPage) {
        return maxPage > 0;
    }

    public static boolean showPrev(int page, int maxPage) {
        return maxPage > 0 && page > 0;
    }

    public static boolean showNext(int page, int maxPage) {
        return maxPage > 0 && page < maxPage;
    }
}
