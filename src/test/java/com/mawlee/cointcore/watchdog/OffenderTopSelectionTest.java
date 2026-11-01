package com.mawlee.cointcore.watchdog;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class OffenderTopSelectionTest {
    @Test void boundedTopMatchesFullSortingIncludingBoundaryTiesForEveryKind() {
        var aggregator = new OffenderAggregator();
        var random = new Random(42);
        for (int i = 0; i < 5000; i++) {
            long nanos = 1 + random.nextInt(20);
            String type = "test:type" + random.nextInt(30);
            if ((i & 1) == 0) aggregator.addBlockEntity(type, "test", "test:world", i, 64, i % 81, nanos);
            else aggregator.addEntity(type, "test", "test:world", i, 64, i % 81, nanos);
        }
        var full = aggregator.topAll(Integer.MAX_VALUE);
        var kinds = new java.util.ArrayList<>(List.of(OffenderAggregator.Kind.values())); kinds.add(null);
        for (var kind : kinds) {
            var expected = full.stream().filter(s -> kind == null || s.kind() == kind)
                    .sorted(Comparator.comparingLong(OffenderAggregator.OffenderSnapshot::totalNanos).reversed()
                            .thenComparing(OffenderAggregator.OffenderSnapshot::typeId)).toList();
            for (int limit : new int[]{0, 1, 3, 10, 51, 20000}) {
                assertEquals(expected.subList(0, Math.min(limit, expected.size())), aggregator.top(kind, limit));
            }
        }
    }

    @Test void registryLabelsAreResolvedOnlyForSelectedCandidatesInsteadOfEveryTrackedObject() {
        var aggregator = new OffenderAggregator();
        for (int i = 0; i < 10000; i++) aggregator.addBlockEntity("test:block", "test", "test:world", i, 64, 0, i + 1);
        var lookups = new AtomicInteger();
        aggregator.setLabels(new WatchdogLabels() {
            public String typeId(Object type) { lookups.incrementAndGet(); return type.toString(); }
            public String dimensionId(Object dimension) { return dimension.toString(); }
        });
        assertEquals(3, aggregator.top(OffenderAggregator.Kind.BLOCK_ENTITY, 3).size());
        assertEquals(3, lookups.get());
        assertTrue(aggregator.top(OffenderAggregator.Kind.BLOCK_ENTITY, 0).isEmpty());
        assertEquals(3, lookups.get());
    }
}
