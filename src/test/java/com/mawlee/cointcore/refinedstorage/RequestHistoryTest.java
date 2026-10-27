package com.mawlee.cointcore.refinedstorage;

import com.mawlee.cointcore.refinedstorage.wireless.RequestHistory;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RequestHistoryTest {
    @Test void repeatingAnOrderMovesItFirstWithoutDuplicatingItsCell() {
        var history = new RequestHistory<String>(3);
        history.remember("stone");
        history.remember("glass");
        history.remember("stone");
        assertEquals(List.of("stone", "glass"), history.snapshot());
    }

    @Test void oldOrdersAreBoundedAndRemovingTheLastOrderClearsTheRow() {
        var history = new RequestHistory<String>(2);
        history.remember("stone");
        history.remember("glass");
        history.remember("iron");
        assertEquals(List.of("iron", "glass"), history.snapshot());
        long version = history.version();
        history.forget("absent");
        assertEquals(version, history.version());
        history.forget("iron");
        history.forget("glass");
        assertTrue(history.snapshot().isEmpty());
        assertTrue(history.version() > version);
    }

    @Test void disconnectClearsOrdersAndSnapshotsCannotModifyHistory() {
        var history = new RequestHistory<String>(2);
        history.remember("stone");
        history.snapshot().clear();
        assertEquals(List.of("stone"), history.snapshot());
        history.clear();
        assertTrue(history.snapshot().isEmpty());
    }
}
