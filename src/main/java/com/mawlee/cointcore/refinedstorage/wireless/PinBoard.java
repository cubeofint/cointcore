package com.mawlee.cointcore.refinedstorage.wireless;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Fixed-position ghost cells: removing one pin never moves neighbouring pins. */
public final class PinBoard<T> {
    public static final int COLUMNS = 9;
    public enum Section { MANUAL, CRAFTING }
    private final ArrayList<T> manual = new ArrayList<>();
    private final ArrayList<T> crafting = new ArrayList<>();
    private int manualRows, craftingRows;
    private long version;

    private ArrayList<T> slots(Section section) { return section == Section.MANUAL ? manual : crafting; }
    public int rows(Section section) { return section == Section.MANUAL ? manualRows : craftingRows; }
    public List<T> cells(Section section) {
        return Collections.unmodifiableList(new ArrayList<>(slots(section).subList(0, rows(section) * COLUMNS)));
    }
    public List<T> storedCells(Section section) { return Collections.unmodifiableList(new ArrayList<>(slots(section))); }
    public long version() { return version; }
    public void addRow(Section section) {
        int count = rows(section) + 1;
        if (section == Section.MANUAL) manualRows = count; else craftingRows = count;
        if (slots(section).size() < count * COLUMNS) slots(section).addAll(Collections.nCopies(COLUMNS, null));
        version++;
    }
    public void removeRow(Section section) {
        if (rows(section) == 0) return;
        if (section == Section.MANUAL) manualRows--; else craftingRows--;
        version++;
    }
    public void set(Section section, int slot, T key) {
        var cells = slots(section);
        if (slot < 0 || slot >= rows(section) * COLUMNS) return;
        if (!Objects.equals(cells.get(slot), key)) { cells.set(slot, key); version++; }
    }
    public void addManual(T key) {
        Objects.requireNonNull(key);
        if (manual.contains(key)) return;
        addToFirstEmpty(Section.MANUAL, key);
    }
    public void rememberCraft(T key) {
        Objects.requireNonNull(key);
        if (crafting.contains(key)) return;
        addToFirstEmpty(Section.CRAFTING, key);
    }
    private void addToFirstEmpty(Section section, T key) {
        var cells = slots(section);
        int empty = cells.indexOf(null);
        // Overflow is retained, but only the player can reveal another row.
        if (empty < 0) {
            empty = cells.size();
            cells.addAll(Collections.nCopies(COLUMNS, null));
        }
        cells.set(empty, key);
        version++;
    }
    public void clear() {
        manual.clear(); crafting.clear(); manualRows = craftingRows = 0; version++;
    }
}
