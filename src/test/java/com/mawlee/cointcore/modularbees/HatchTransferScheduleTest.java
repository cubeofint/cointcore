package com.mawlee.cointcore.modularbees;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class HatchTransferScheduleTest {
    @Test void exactlyOneTransferPerPeriodIncludingAfterReloadAndWithNegativePositions() {
        for (long position : new long[] {0, 1, -1, Long.MIN_VALUE, 99887766554L}) {
            for (int interval : new int[] {1, 3, 100, 1200}) {
                int count = 0;
                for (long tick = 10000; tick < 10000 + interval * 3L; tick++)
                    if (HatchTransferSchedule.isDue(tick, position, interval)) count++;
                assertEquals(3, count);
            }
        }
    }

    @Test void neighbouringHatchesDoNotAllSendOnTheSameTick() {
        var phases = new HashSet<Integer>();
        for (long position = 0; position < 100; position++) {
            for (int tick = 0; tick < 100; tick++)
                if (HatchTransferSchedule.isDue(tick, position, 100)) phases.add(tick);
        }
        assertEquals(100, phases.size());
        assertTrue(HatchTransferSchedule.isDue(42, -13, 0));
    }
}
