package com.mawlee.cointcore.logisticsnetworks;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class FairDispatchBudgetTest {
    @Test void deferredNetworksDoNotAllBypassTheNextTicksBudget() {
        var budget = new FairDispatchBudget<String>();
        assertTrue(budget.tryStart("a", 1, 0, 6));
        assertFalse(budget.tryStart("b", 1, 7, 6));
        assertFalse(budget.tryStart("c", 1, 7, 6));
        var pending = new LinkedHashSet<>(List.of("a", "b", "c"));
        assertEquals(List.of("b", "c", "a"), new ArrayList<>(budget.order(pending)));
        assertTrue(budget.tryStart("b", 2, 100, 6));
        assertFalse(budget.tryStart("c", 2, 107, 6));
        assertFalse(budget.tryStart("a", 2, 107, 6));
        assertEquals(List.of("c", "a", "b"), new ArrayList<>(budget.order(pending)));
    }

    @Test void continuouslyBusyNetworksAllGetServedEvenWhenOneConsumesTheWholeBudget() {
        var budget = new FairDispatchBudget<Integer>();
        var networks = new LinkedHashSet<Integer>();
        for (int id = 0; id < 100; id++) networks.add(id);
        var served = new LinkedHashSet<Integer>();
        for (int tick = 0; tick < 100; tick++) {
            long now = tick * 100L;
            int runs = 0;
            for (int network : budget.order(networks)) {
                if (budget.tryStart(network, tick, now, 6)) {
                    served.add(network);
                    runs++;
                    now += 7;
                }
            }
            assertEquals(1, runs, "One atomic network may exceed the budget; no retry may bypass it");
        }
        assertEquals(networks, served);
    }

    @Test void smallNetworksCanShareATickAndDeletedNetworksDoNotRemainQueued() {
        var budget = new FairDispatchBudget<String>();
        assertTrue(budget.tryStart("a", 1, 0, 6));
        assertTrue(budget.tryStart("b", 1, 2, 6));
        assertFalse(budget.tryStart("deleted", 1, 6, 6));
        assertEquals(Set.of("a", "b"), budget.order(Set.of("a", "b")));
        budget.clear();
        assertTrue(budget.tryStart("a", 1, 100, 6));
    }
}
