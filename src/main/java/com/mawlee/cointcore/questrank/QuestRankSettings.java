package com.mawlee.cointcore.questrank;

import java.util.Map;

/**
 * Parsed quest-rank config. No NeoForge types so unit tests can construct it.
 */
public record QuestRankSettings(
        boolean enabled,
        int sweepIntervalSeconds,
        boolean resetRankQuestTeamProgress,
        QuestRankTable table
) {
    public static final int DEFAULT_SWEEP_INTERVAL_SECONDS = 20;

    public QuestRankSettings {
        table = table == null ? new QuestRankTable(Map.of()) : table;
        sweepIntervalSeconds = clampSweep(sweepIntervalSeconds);
    }

    public static QuestRankSettings defaults() {
        return from(true, DEFAULT_SWEEP_INTERVAL_SECONDS, true, QuestRankTable.DEFAULT_QUESTS);
    }

    public static QuestRankSettings from(
            Boolean enabled,
            Integer sweepIntervalSeconds,
            Map<String, String> quests
    ) {
        return from(enabled, sweepIntervalSeconds, true, quests);
    }

    public static QuestRankSettings from(
            Boolean enabled,
            Integer sweepIntervalSeconds,
            Boolean resetRankQuestTeamProgress,
            Map<String, String> quests
    ) {
        Map<String, String> raw = quests != null ? quests : QuestRankTable.DEFAULT_QUESTS;
        return new QuestRankSettings(
                enabled == null || enabled,
                clampSweep(sweepIntervalSeconds),
                resetRankQuestTeamProgress == null || resetRankQuestTeamProgress,
                QuestRankTable.parse(raw)
        );
    }

    static int clampSweep(Integer value) {
        if (value == null || value < 5) {
            return DEFAULT_SWEEP_INTERVAL_SECONDS;
        }
        return Math.min(3600, value);
    }

    public int sweepIntervalTicks() {
        return sweepIntervalSeconds * 20;
    }
}
