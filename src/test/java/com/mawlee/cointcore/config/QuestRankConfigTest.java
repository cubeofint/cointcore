package com.mawlee.cointcore.config;

import com.mawlee.cointcore.questrank.QuestRankSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRankConfigTest {
    @Test
    void missingResetFlagDefaultsOn() {
        QuestRankConfig.FileData data = new QuestRankConfig.FileData();
        data.enabled = true;
        data.sweepIntervalSeconds = 20;
        data.quests = QuestRankConfig.FileData.defaultQuests();
        QuestRankSettings settings = QuestRankConfig.fromFile(data);
        assertTrue(settings.resetRankQuestTeamProgress());
        assertEquals(9, settings.table().size());
    }

    @Test
    void defaultsFileEnablesReset() {
        QuestRankSettings settings = QuestRankConfig.fromFile(QuestRankConfig.FileData.defaults());
        assertTrue(settings.enabled());
        assertTrue(settings.resetRankQuestTeamProgress());
    }

    @Test
    void explicitFalseDisablesReset() {
        QuestRankConfig.FileData data = QuestRankConfig.FileData.defaults();
        data.resetRankQuestTeamProgress = false;
        assertFalse(QuestRankConfig.fromFile(data).resetRankQuestTeamProgress());
    }
}
