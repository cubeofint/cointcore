package com.mawlee.cointcore.reminder;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegularScheduleTest {
    private static final ZoneId MSK = ZoneId.of("Europe/Moscow");

    private static long at(int hour, int minute) {
        return ZonedDateTime.of(2026, 10, 21, hour, minute, 0, 0, MSK).toInstant().toEpochMilli();
    }

    @Test
    void parsesDailyAndAfterStart() {
        assertEquals(Optional.of(new RegularSchedule.Daily(4)), RegularSchedule.parse("4/д"));
        assertEquals(Optional.of(new RegularSchedule.Daily(2)), RegularSchedule.parse("2/D"));
        assertEquals(Optional.of(new RegularSchedule.AfterStart(90)), RegularSchedule.parse("+1ч30м"));
        assertEquals(Optional.of(new RegularSchedule.AfterStart(60)), RegularSchedule.parse("+1h"));
        assertEquals("+1ч30м", new RegularSchedule.AfterStart(90).describe());
        assertEquals("+45м", new RegularSchedule.AfterStart(45).describe());
        assertEquals("4/д", new RegularSchedule.Daily(4).describe());
        assertTrue(RegularSchedule.parse("0/д").isEmpty());
        assertTrue(RegularSchedule.parse("+").isEmpty());
        assertTrue(RegularSchedule.parse("1ч").isEmpty());
        assertTrue(RegularSchedule.parse("Привет").isEmpty());
    }

    @Test
    void dailySlotsFollowWallClock() {
        RegularSchedule four = new RegularSchedule.Daily(4);
        assertEquals(at(12, 0), four.latestSlot(at(13, 30), 0L, MSK));
        assertEquals(at(18, 0), four.nextSlot(at(13, 30), 0L, MSK));
        assertEquals(at(18, 0), four.latestSlot(at(18, 0), 0L, MSK));
        assertEquals(at(0, 0) + 24L * 3_600_000L, four.nextSlot(at(23, 0), 0L, MSK));
        // 1440 / 7 is not whole: slot 1 is at 03:25 and must count from that minute on.
        assertEquals(at(3, 25), new RegularSchedule.Daily(7).latestSlot(at(3, 25), 0L, MSK));
    }

    @Test
    void sameSlotGoesOutFifteenMinutesApartInIdOrder() {
        long start = at(10, 0);
        RegularPlanner planner = new RegularPlanner(start);
        RegularSchedule hourAfter = new RegularSchedule.AfterStart(60);
        List<RegularPlanner.Item> items = List.of(
                new RegularPlanner.Item(3, hourAfter),
                new RegularPlanner.Item(1, hourAfter),
                new RegularPlanner.Item(2, hourAfter)
        );
        long gap = 15L * 60_000L;
        assertTrue(planner.poll(items, start, MSK, gap).isEmpty());
        assertTrue(planner.poll(items, at(10, 59), MSK, gap).isEmpty());
        assertEquals(1, planner.poll(items, at(11, 0), MSK, gap).orElseThrow().id());
        assertTrue(planner.poll(items, at(11, 14), MSK, gap).isEmpty());
        assertEquals(2, planner.poll(items, at(11, 15), MSK, gap).orElseThrow().id());
        assertEquals(3, planner.poll(items, at(11, 30), MSK, gap).orElseThrow().id());
        assertTrue(planner.poll(items, at(12, 30), MSK, gap).isEmpty());
    }

    @Test
    void passedDailySlotIsNotSentOnStart() {
        RegularPlanner planner = new RegularPlanner(at(13, 30));
        List<RegularPlanner.Item> items = List.of(new RegularPlanner.Item(1, new RegularSchedule.Daily(4)));
        assertTrue(planner.poll(items, at(13, 30), MSK, 0L).isEmpty());
        assertTrue(planner.poll(items, at(17, 59), MSK, 0L).isEmpty());
        assertEquals(1, planner.poll(items, at(18, 0), MSK, 0L).orElseThrow().id());
        assertTrue(planner.poll(items, at(18, 1), MSK, 0L).isEmpty());
    }

    @Test
    void textSupportsHexAndMiniMessage() {
        assertEquals("§x§f§f§a§a§0§0Hi &lB", RegularMessageText.toLegacy("&#FFAA00Hi &lB"));
        assertEquals("§lB§r", RegularMessageText.toLegacy("<bold>B</bold>"));
    }
}
