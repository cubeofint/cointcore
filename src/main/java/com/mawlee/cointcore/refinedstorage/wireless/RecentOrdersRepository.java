package com.mawlee.cointcore.refinedstorage.wireless;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.resource.repository.*;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.grid.view.GridResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.world.item.Items;
import java.util.*;

/** Ghost rows precede the ordinary RS view, independently of its search and sorting. */
public final class RecentOrdersRepository implements ResourceRepository<GridResource> {
    private final ResourceRepository<GridResource> delegate;
    private long version = -1, epoch = -1;
    private List<GridResource> pinned = List.of();
    private final List<GridResource> view = new AbstractList<>() {
        @Override public GridResource get(int index) {
            refresh();
            return index < pinned.size() ? pinned.get(index) : delegate.getViewList().get(index - pinned.size());
        }
        @Override public int size() { refresh(); return pinned.size() + delegate.getViewList().size(); }
    };
    public RecentOrdersRepository(ResourceRepository<GridResource> delegate) { this.delegate = delegate; }

    private void refresh() {
        var board = PinStorage.board();
        if (version == board.version() && epoch == PinStorage.epoch()) return;
        version = board.version();
        epoch = PinStorage.epoch();
        var entries = new ArrayList<GridResource>();
        var mapper = RefinedStorageApi.INSTANCE.getGridResourceRepositoryMapper();
        GridResource empty = mapper.apply(new ItemResource(Items.AIR));
        for (var section : PinBoard.Section.values()) {
            var cells = board.cells(section);
            for (int slot = 0; slot < cells.size(); slot++) {
                var key = cells.get(slot);
                GridResource resource = key == null ? empty : mapper.apply(key);
                entries.add(new RequestedGridResource(key, resource == null ? empty : resource, section, slot));
            }
        }
        pinned = List.copyOf(entries);
    }

    @Override public List<GridResource> getViewList() { return view; }
    @Override public void setListener(Runnable listener) { delegate.setListener(listener); }
    @Override public void setSort(Comparator<GridResource> sort, SortingDirection direction) { delegate.setSort(sort, direction); }
    @Override public boolean setPreventSorting(boolean prevent) { return delegate.setPreventSorting(prevent); }
    @Override public ResourceRepositoryFilter<GridResource> setFilterAndSort(ResourceRepositoryFilter<GridResource> filter) { return delegate.setFilterAndSort(filter); }
    @Override public long getAmount(ResourceKey resource) { return delegate.getAmount(resource); }
    @Override public boolean isSticky(ResourceKey resource) { return delegate.isSticky(resource); }
    @Override public void sort() { delegate.sort(); }
    @Override public void update(ResourceKey resource, long amount) { delegate.update(resource, amount); }
    @Override public MutableResourceList copyBackingList() { return delegate.copyBackingList(); }
    @Override public void clear() { delegate.clear(); }
}
