package com.mawlee.cointcore.functionalstorage;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Functional Storage's storage controller checks {@code drawers.getItemHandlers().contains(handler)} on every
 * {@code getStackInSlot}/{@code insertItem}/{@code extractItem}. Inserters that scan every slot (Logistics Networks,
 * {@code ItemHandlerHelper#insertItemStacked}) then cost slots × drawers per item: 5% of the server tick on ATM10.
 * This answers the same question from an identity set, rebuilt whenever the controller swaps or grows its list.
 */
public final class ControllerMembership {
    /** The handler list it was built from, its size at the time, and the handlers in it by identity. */
    public record Snapshot(List<?> list, int size, Set<Object> members) {
    }

    private ControllerMembership() {
    }

    /** Same answer as {@code handlers.contains(handler)}; returns the snapshot to keep for the next call. */
    public static Snapshot refresh(Snapshot snapshot, List<?> handlers) {
        if (snapshot != null && snapshot.list() == handlers && snapshot.size() == handlers.size()) {
            return snapshot;
        }
        Set<Object> members = Collections.newSetFromMap(new IdentityHashMap<>(handlers.size() * 2));
        members.addAll(handlers);
        return new Snapshot(handlers, handlers.size(), members);
    }
}
