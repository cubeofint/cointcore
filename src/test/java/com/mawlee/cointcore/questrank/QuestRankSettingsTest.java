package com.mawlee.cointcore.questrank;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRankSettingsTest {
    @Test
    void missingKeysKeepEnabledDefaultsAndNineMappings() {
        QuestRankSettings settings = QuestRankSettings.from(null, null, null);
        assertTrue(settings.enabled());
        assertEquals(20, settings.sweepIntervalSeconds());
        assertEquals(400, settings.sweepIntervalTicks());
        assertEquals(9, settings.table().size());
        assertEquals(
                "godofatm",
                settings.table().groupFor(QuestRankIds.parseHex("330D102D30E63E2C").orElseThrow()).orElseThrow()
        );
    }

    @Test
    void explicitValuesAreHonoredIncludingEmptyMap() {
        QuestRankSettings settings = QuestRankSettings.from(
                false,
                15,
                Map.of("6491B39FBC0C53B2", "Titan")
        );
        assertFalse(settings.enabled());
        assertEquals(15, settings.sweepIntervalSeconds());
        assertEquals(1, settings.table().size());
        assertEquals(
                "titan",
                settings.table().groupFor(QuestRankIds.parseHex("6491B39FBC0C53B2").orElseThrow()).orElseThrow()
        );

        assertTrue(QuestRankSettings.from(true, 60, Map.of()).table().isEmpty());
    }

    @Test
    void sweepIntervalIsClamped() {
        assertEquals(20, QuestRankSettings.clampSweep(1));
        assertEquals(20, QuestRankSettings.clampSweep(null));
        assertEquals(15, QuestRankSettings.clampSweep(15));
        assertEquals(3600, QuestRankSettings.clampSweep(99_999));
    }

    @Test
    void defaultSweepIsTwentySecondsForLoginPlusSweepPath() {
        assertEquals(20, QuestRankSettings.DEFAULT_SWEEP_INTERVAL_SECONDS);
        assertEquals(400, QuestRankSettings.defaults().sweepIntervalTicks());
    }
}
