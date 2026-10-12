package com.mawlee.cointcore.questrank;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable quest-id → LuckPerms group map. Quest ids are unsigned longs (hex in config).
 */
public record QuestRankTable(Map<Long, String> questToGroup) {
    public static final Map<String, String> DEFAULT_QUESTS = defaultQuests();

    public QuestRankTable {
        questToGroup = questToGroup == null ? Map.of() : Map.copyOf(questToGroup);
    }

    public static QuestRankTable defaults() {
        return parse(DEFAULT_QUESTS);
    }

    public static QuestRankTable parse(Map<String, String> raw) {
        if (raw == null || raw.isEmpty()) {
            return new QuestRankTable(Map.of());
        }
        Map<Long, String> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            var questId = QuestRankIds.parseHex(entry.getKey());
            String group = normalizeGroup(entry.getValue());
            if (questId.isPresent() && group != null) {
                parsed.put(questId.getAsLong(), group);
            }
        }
        return new QuestRankTable(parsed);
    }

    public Optional<String> groupFor(long questId) {
        return Optional.ofNullable(questToGroup.get(questId));
    }

    public Set<Long> questIds() {
        return questToGroup.keySet();
    }

    public int size() {
        return questToGroup.size();
    }

    public boolean isEmpty() {
        return questToGroup.isEmpty();
    }

    public static String normalizeGroup(String group) {
        if (group == null || group.isBlank()) {
            return null;
        }
        return group.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> defaultQuests() {
        Map<String, String> quests = new LinkedHashMap<>();
        quests.put("539D2768DC388BB2", "darwinawards");
        quests.put("3FABD06F3F6D3BC3", "explorer");
        quests.put("03610B08578C5710", "engineer");
        quests.put("4E6C254EF6E26DB2", "atm");
        quests.put("5F2E99EA50A3E1CA", "arcanist");
        quests.put("668583DA736FE3EE", "bosskiller");
        quests.put("6491B39FBC0C53B2", "titan");
        quests.put("22E4E7DE8AF39F1D", "jesus");
        quests.put("330D102D30E63E2C", "godofatm");
        return Map.copyOf(quests);
    }
}
