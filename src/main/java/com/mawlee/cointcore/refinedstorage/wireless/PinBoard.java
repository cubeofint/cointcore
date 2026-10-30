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
    private long version;

    private ArrayList<T> slots(Section section) { return section == Section.MANUAL ? manual : crafting; }
    public int rows(Section section) { return slots(section).size() / COLUMNS; }
    public List<T> cells(Section section) { return Collections.unmodifiableList(new ArrayList<>(slots(section))); }
    public long version() { return version; }
    public void addRow(Section section) {
        slots(section).addAll(Collections.nCopies(COLUMNS, null));
        version++;
    }
    public void removeRow(Section section) {
        var cells = slots(section);
        if (cells.isEmpty()) return;
        cells.subList(cells.size() - COLUMNS, cells.size()).clear();
        version++;
    }
    public void set(Section section, int slot, T key) {
        var cells = slots(section);
        if (slot < 0 || slot >= cells.size()) return;
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
        if (empty < 0) { empty = cells.size(); addRow(section); }
        set(section, empty, key);
    }
    public void clear() {
        manual.clear(); crafting.clear(); version++;
    }
}
