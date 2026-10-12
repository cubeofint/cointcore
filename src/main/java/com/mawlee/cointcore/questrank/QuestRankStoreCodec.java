package com.mawlee.cointcore.questrank;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Map-shaped codec for {@link QuestRankCompletionStore} so unit tests can round-trip
 * the same keys as world SavedData without Minecraft NBT types.
 */
public final class QuestRankStoreCodec {
    private QuestRankStoreCodec() {
    }

    public static Map<String, Object> saveMap(QuestRankCompletionStore store) {
        List<Map<String, Object>> players = new ArrayList<>();
        for (Map.Entry<UUID, Set<Long>> entry : store.snapshot().entrySet()) {
            Map<String, Object> player = new LinkedHashMap<>();
            player.put("player_id", entry.getKey().toString());
            List<String> quests = new ArrayList<>();
            for (long questId : entry.getValue()) {
                quests.add(QuestRankIds.toHex(questId));
            }
            player.put("quests", quests);
            players.add(player);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("players", players);
        return root;
    }

    public static void loadMap(QuestRankCompletionStore store, Map<String, Object> root) {
        Map<UUID, Set<Long>> loaded = new LinkedHashMap<>();
        if (root == null) {
            store.replaceAll(loaded);
            return;
        }
        Object rawPlayers = root.get("players");
        if (!(rawPlayers instanceof List<?> players)) {
            store.replaceAll(loaded);
            return;
        }
        for (Object rawPlayer : players) {
            if (!(rawPlayer instanceof Map<?, ?> playerMap)) {
                continue;
            }
            Object rawId = playerMap.get("player_id");
            if (!(rawId instanceof String idText) || idText.isBlank()) {
                continue;
            }
            UUID playerId;
            try {
                playerId = UUID.fromString(idText);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            Set<Long> quests = new LinkedHashSet<>();
            Object rawQuests = playerMap.get("quests");
            if (rawQuests instanceof List<?> questList) {
                for (Object rawQuest : questList) {
                    if (rawQuest instanceof String hex) {
                        QuestRankIds.parseHex(hex).ifPresent(quests::add);
                    }
                }
            }
            if (!quests.isEmpty()) {
                loaded.put(playerId, quests);
            }
        }
        store.replaceAll(loaded);
    }
}
