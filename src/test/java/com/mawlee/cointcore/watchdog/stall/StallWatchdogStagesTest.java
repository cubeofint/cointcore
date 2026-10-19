package com.mawlee.cointcore.watchdog.stall;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StallWatchdogStagesTest {
    private static final StallWatchdogStages.Settings SETTINGS = new StallWatchdogStages.Settings(
            5_000L,
            5_000L,
            6,
            10_000L,
            15_000L,
            true
    );

    @Test
    void staysIdleUntilWarnThreshold() {
        StallWatchdogStages stages = new StallWatchdogStages();
        assertEquals(List.of(), stages.evaluate(1_000L, 900L, SETTINGS));
        assertFalse(stages.inStall());
    }

    @Test
    void walksWarnNotifyBreakThenRecovers() {
        StallWatchdogStages stages = new StallWatchdogStages();
        long start = 100_000L;
        List<StallWatchdogStages.Action> first = stages.evaluate(start + 5_000L, start, SETTINGS);
        assertTrue(first.contains(StallWatchdogStages.Action.WARN_DUMP));
        assertEquals(1, stages.dumps());

        List<StallWatchdogStages.Action> quiet = stages.evaluate(start + 7_000L, start, SETTINGS);
        assertEquals(List.of(StallWatchdogStages.Action.NONE), quiet);

        List<StallWatchdogStages.Action> second = stages.evaluate(start + 10_000L, start, SETTINGS);
        assertTrue(second.contains(StallWatchdogStages.Action.REPEAT_DUMP));
        assertTrue(second.contains(StallWatchdogStages.Action.NOTIFY_STAFF));

        List<StallWatchdogStages.Action> brk = stages.evaluate(start + 15_000L, start, SETTINGS);
        assertTrue(brk.contains(StallWatchdogStages.Action.REQUEST_BREAK));
        assertTrue(stages.breakRequested());

        List<StallWatchdogStages.Action> recovered = stages.evaluate(start + 15_100L, start + 15_050L, SETTINGS);
        assertTrue(recovered.contains(StallWatchdogStages.Action.RECOVERED));
        assertFalse(stages.inStall());
        assertTrue(stages.lastStallDurationMs() >= 15_000L);
    }

    @Test
    void collapsesIdenticalStacks() {
        StallWatchdogStages stages = new StallWatchdogStages();
        assertFalse(stages.noteStack("at foo\n"));
        assertTrue(stages.noteStack("at foo\n"));
        assertEquals(1, stages.collapsedStacks());
        assertFalse(stages.noteStack("at bar\n"));
        assertEquals(0, stages.collapsedStacks());
    }

    @Test
    void breakCanBeDisabled() {
        StallWatchdogStages stages = new StallWatchdogStages();
        StallWatchdogStages.Settings noBreak = new StallWatchdogStages.Settings(
                5_000L, 5_000L, 6, 10_000L, 15_000L, false
        );
        stages.evaluate(20_000L, 0L, noBreak);
        List<StallWatchdogStages.Action> late = stages.evaluate(20_000L, 1L, noBreak);
        assertFalse(late.contains(StallWatchdogStages.Action.REQUEST_BREAK));
    }
}
