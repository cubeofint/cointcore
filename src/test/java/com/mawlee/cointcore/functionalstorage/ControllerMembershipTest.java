package com.mawlee.cointcore.functionalstorage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControllerMembershipTest {
    @Test
    void reusesTheSnapshotWhileTheListIsUnchanged() {
        Object drawer = new Object();
        List<Object> handlers = new ArrayList<>(List.of(drawer));
        ControllerMembership.Snapshot first = ControllerMembership.refresh(null, handlers);
        assertTrue(first.members().contains(drawer));
        assertSame(first, ControllerMembership.refresh(first, handlers));
    }

    @Test
    void rebuildsWhenTheControllerSwapsOrGrowsItsList() {
        Object drawer = new Object();
        Object added = new Object();
        List<Object> handlers = new ArrayList<>(List.of(drawer));
        ControllerMembership.Snapshot first = ControllerMembership.refresh(null, handlers);

        handlers.add(added);
        ControllerMembership.Snapshot grown = ControllerMembership.refresh(first, handlers);
        assertTrue(grown.members().contains(added));

        List<Object> rebuilt = new ArrayList<>(List.of(added));
        ControllerMembership.Snapshot swapped = ControllerMembership.refresh(grown, rebuilt);
        assertFalse(swapped.members().contains(drawer));
        assertTrue(swapped.members().contains(added));
    }
}
