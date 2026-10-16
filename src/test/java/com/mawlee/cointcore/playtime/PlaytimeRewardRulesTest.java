package com.mawlee.cointcore.playtime;

import com.mawlee.cointcore.config.PlaytimeRewardParse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaytimeRewardRulesTest {
    @Test
    void leftoverCarriesUntilFullInterval() {
        long hour = PlaytimeRewardRules.intervalMillis(60);
        PlaytimeRewardRules.Accrual first = PlaytimeRewardRules.accrue(0L, hour - 1L, hour);
        assertEquals(0, first.intervals());
        assertEquals(hour - 1L, first.remainingMs());

        PlaytimeRewardRules.Accrual second = PlaytimeRewardRules.accrue(first.remainingMs(), 2L, hour);
        assertEquals(1, second.intervals());
        assertEquals(1L, second.remainingMs());
    }

    @Test
    void multipleIntervalsCreditTogether() {
        long hour = PlaytimeRewardRules.intervalMillis(60);
        PlaytimeRewardRules.Accrual accrual = PlaytimeRewardRules.accrue(0L, hour * 3L + 12L, hour);
        assertEquals(3, accrual.intervals());
        assertEquals(12L, accrual.remainingMs());
        assertEquals(12L, PlaytimeRewardRules.creditAmount(3, 4L));
    }

    @Test
    void parseDefaultsAndClamps() {
        PlaytimeRewardParse.Parsed defaults = PlaytimeRewardParse.parseJson("{}");
        assertTrue(defaults.enabled());
        assertEquals(4L, defaults.amount());
        assertEquals(60, defaults.intervalMinutes());

        PlaytimeRewardParse.Parsed custom = PlaytimeRewardParse.parseJson("""
                {
                  "enabled": false,
                  "amount": 10,
                  "intervalMinutes": 30
                }
                """);
        assertFalse(custom.enabled());
        assertEquals(10L, custom.amount());
        assertEquals(30, custom.intervalMinutes());

        PlaytimeRewardParse.Parsed clamped = PlaytimeRewardParse.parseJson("""
                {
                  "amount": -2,
                  "intervalMinutes": 0
                }
                """);
        assertEquals(0L, clamped.amount());
        assertEquals(1, clamped.intervalMinutes());
    }
}
