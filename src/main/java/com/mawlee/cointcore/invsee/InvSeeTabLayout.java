package com.mawlee.cointcore.invsee;

import java.util.ArrayList;
import java.util.List;

/**
 * Packs InvSee tab buttons so labels never overlap. Prefers wrapping into at most
 * two rows; otherwise one row with a scroll window.
 */
public final class InvSeeTabLayout {
    public static final int MAX_WRAP_ROWS = 2;
    public static final int ARROW_WIDTH = 16;

    public record Placement(int tabIndex, int x, int y, int width, int row) {
    }

    public record Result(
            List<Placement> placements,
            int rows,
            boolean scrolled,
            boolean canScrollLeft,
            boolean canScrollRight,
            int areaHeight
    ) {
    }

    private InvSeeTabLayout() {
    }

    public static int tabWidth(int textWidth, int padding, int minWidth) {
        return Math.max(minWidth, Math.max(0, textWidth) + Math.max(0, padding));
    }

    public static Result compute(
            int[] widths,
            int availableWidth,
            int scrollIndex,
            int tabHeight,
            int gap
    ) {
        int count = widths == null ? 0 : widths.length;
        int safeAvail = Math.max(1, availableWidth);
        int safeHeight = Math.max(1, tabHeight);
        int safeGap = Math.max(0, gap);
        if (count == 0) {
            return new Result(List.of(), 1, false, false, false, safeHeight);
        }

        int[] clamped = new int[count];
        for (int i = 0; i < count; i++) {
            clamped[i] = Math.min(safeAvail, Math.max(1, widths[i]));
        }

        List<List<Integer>> wrapped = wrap(clamped, safeAvail, safeGap);
        if (wrapped.size() <= MAX_WRAP_ROWS) {
            List<Placement> placements = new ArrayList<>();
            int y = 0;
            for (int row = 0; row < wrapped.size(); row++) {
                int x = 0;
                for (int index : wrapped.get(row)) {
                    placements.add(new Placement(index, x, y, clamped[index], row));
                    x += clamped[index] + safeGap;
                }
                y += safeHeight + safeGap;
            }
            int rows = wrapped.size();
            int area = rows * safeHeight + Math.max(0, rows - 1) * safeGap;
            return new Result(List.copyOf(placements), rows, false, false, false, area);
        }

        int scrollAvail = Math.max(1, safeAvail - ARROW_WIDTH * 2 - safeGap * 2);
        int start = Math.max(0, Math.min(count - 1, scrollIndex));
        while (start > 0 && !windowFits(clamped, start - 1, count, scrollAvail, safeGap)) {
            start--;
        }
        while (start < count - 1 && windowFits(clamped, start + 1, count, scrollAvail, safeGap)) {
            // keep the requested start if it already fits; do not auto-advance
            break;
        }
        if (!windowFits(clamped, start, count, scrollAvail, safeGap) && start < count - 1) {
            while (start < count - 1 && !windowFits(clamped, start, count, scrollAvail, safeGap)) {
                start++;
            }
        }

        List<Placement> placements = new ArrayList<>();
        int x = ARROW_WIDTH + safeGap;
        for (int index = start; index < count; index++) {
            int next = x + clamped[index];
            if (index > start && next > ARROW_WIDTH + safeGap + scrollAvail) {
                break;
            }
            if (clamped[index] > scrollAvail && index == start) {
                placements.add(new Placement(index, x, 0, scrollAvail, 0));
                start = index;
                break;
            }
            if (next > ARROW_WIDTH + safeGap + scrollAvail) {
                break;
            }
            placements.add(new Placement(index, x, 0, clamped[index], 0));
            x = next + safeGap;
        }
        boolean left = start > 0;
        int lastShown = placements.isEmpty() ? start : placements.getLast().tabIndex();
        boolean right = lastShown < count - 1;
        return new Result(List.copyOf(placements), 1, true, left, right, safeHeight);
    }

    private static boolean windowFits(int[] widths, int start, int count, int avail, int gap) {
        int x = 0;
        for (int i = start; i < count; i++) {
            if (i > start) {
                x += gap;
            }
            x += widths[i];
            if (x > avail) {
                return false;
            }
        }
        return true;
    }

    private static List<List<Integer>> wrap(int[] widths, int avail, int gap) {
        List<List<Integer>> rows = new ArrayList<>();
        List<Integer> current = new ArrayList<>();
        int used = 0;
        for (int i = 0; i < widths.length; i++) {
            int extra = current.isEmpty() ? widths[i] : widths[i] + gap;
            if (!current.isEmpty() && used + extra > avail) {
                rows.add(current);
                current = new ArrayList<>();
                used = 0;
                extra = widths[i];
            }
            current.add(i);
            used += extra;
        }
        if (!current.isEmpty()) {
            rows.add(current);
        }
        return rows;
    }
}
