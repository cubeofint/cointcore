package com.mawlee.cointcore.config;

import com.google.gson.Gson;
import com.mawlee.cointcore.questrank.QuestRankIds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRankConfigTest {
    private static final Gson GSON = new Gson();

    @Test
    void missingKeysKeepEnabledDefaultsAndNineMappings() {
        QuestRankConfig.FileData missing = GSON.fromJson("{}", QuestRankConfig.FileData.class);
        QuestRankConfig.Settings settings = QuestRankConfig.Settings.from(missing);
        assertTrue(settings.enabled());
        assertEquals(60, settings.sweepIntervalSeconds());
        assertEquals(9, settings.table().size());
        assertEquals(
                "godofatm",
                settings.table().groupFor(QuestRankIds.parseHex("330D102D30E63E2C").orElseThrow()).orElseThrow()
        );
    }

    @Test
    void explicitValuesAreHonoredIncludingEmptyMap() {
        QuestRankConfig.FileData data = GSON.fromJson(
                "{\"enabled\":false,\"sweepIntervalSeconds\":15,\"quests\":{\"6491B39FBC0C53B2\":\"Titan\"}}",
                QuestRankConfig.FileData.class
        );
        QuestRankConfig.Settings settings = QuestRankConfig.Settings.from(data);
        assertFalse(settings.enabled());
        assertEquals(15, settings.sweepIntervalSeconds());
        assertEquals(1, settings.table().size());
        assertEquals(
                "titan",
                settings.table().groupFor(QuestRankIds.parseHex("6491B39FBC0C53B2").orElseThrow()).orElseThrow()
        );

        QuestRankConfig.FileData emptyQuests = GSON.fromJson(
                "{\"enabled\":true,\"quests\":{}}",
                QuestRankConfig.FileData.class
        );
        assertTrue(QuestRankConfig.Settings.from(emptyQuests).table().isEmpty());
    }

    @Test
    void sweepIntervalIsClamped() {
        QuestRankConfig.FileData tiny = GSON.fromJson(
                "{\"sweepIntervalSeconds\":1}",
                QuestRankConfig.FileData.class
        );
        assertEquals(60, QuestRankConfig.Settings.from(tiny).sweepIntervalSeconds());

        QuestRankConfig.FileData huge = GSON.fromJson(
                "{\"sweepIntervalSeconds\":99999}",
                QuestRankConfig.FileData.class
        );
        assertEquals(3600, QuestRankConfig.Settings.from(huge).sweepIntervalSeconds());
    }
}
