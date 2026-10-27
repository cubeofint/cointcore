package com.mawlee.cointcore.refinedstorage.wireless;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/** Bounded recent requests, newest first, with one entry per resource. */
public final class RequestHistory<T> {
    private final int limit;
    private final LinkedList<T> resources = new LinkedList<>();
    private long version;

    public RequestHistory(int limit) {
        if (limit < 1) throw new IllegalArgumentException("History must have a positive limit");
        this.limit = limit;
    }

    public void remember(T resource) {
        java.util.Objects.requireNonNull(resource);
        resources.remove(resource);
        resources.addFirst(resource);
        while (resources.size() > limit) resources.removeLast();
        version++;
    }

    public void forget(T resource) { if (resources.remove(resource)) version++; }
    public void clear() { if (!resources.isEmpty()) { resources.clear(); version++; } }
    public List<T> snapshot() { return new ArrayList<>(resources); }
    public long version() { return version; }
}
