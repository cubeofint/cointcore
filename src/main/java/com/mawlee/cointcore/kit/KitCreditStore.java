package com.mawlee.cointcore.kit;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory kit-credit ledger keyed by player UUID.
 * Credits never expire: a purchased use stays until it is spent or cleared.
 */
public final class KitCreditStore {
    private final Map<UUID, Map<String, Integer>> creditsByPlayer = new HashMap<>();

    public synchronized int get(UUID playerId, String kitName) {
        if (playerId == null) {
            return 0;
        }
        Map<String, Integer> kits = creditsByPlayer.get(playerId);
        if (kits == null) {
            return 0;
        }
        String key = normalizeKitName(kitName);
        if (key == null) {
            return 0;
        }
        return Math.max(0, kits.getOrDefault(key, 0));
    }

    public synchronized int set(UUID playerId, String kitName, int amount) {
        if (playerId == null) {
            return 0;
        }
        String key = normalizeKitName(kitName);
        if (key == null) {
            return 0;
        }
        int normalized = Math.max(0, amount);
        if (normalized == 0) {
            Map<String, Integer> kits = creditsByPlayer.get(playerId);
            if (kits != null) {
                kits.remove(key);
                if (kits.isEmpty()) {
                    creditsByPlayer.remove(playerId);
                }
            }
            return 0;
        }
        creditsByPlayer.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(key, normalized);
        return normalized;
    }

    public synchronized int add(UUID playerId, String kitName, int amount) {
        if (amount <= 0) {
            return get(playerId, kitName);
        }
        int current = get(playerId, kitName);
        int next = amount > Integer.MAX_VALUE - current ? Integer.MAX_VALUE : current + amount;
        return set(playerId, kitName, next);
    }

    public synchronized int take(UUID playerId, String kitName, int amount) {
        if (amount <= 0) {
            return get(playerId, kitName);
        }
        return set(playerId, kitName, Math.max(0, get(playerId, kitName) - amount));
    }

    public synchronized Map<String, Integer> all(UUID playerId) {
        if (playerId == null) {
            return Map.of();
        }
        Map<String, Integer> kits = creditsByPlayer.get(playerId);
        if (kits == null || kits.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(kits);
    }

    public synchronized Map<String, Integer> clearAll(UUID playerId) {
        Map<String, Integer> previous = all(playerId);
        if (playerId != null) {
            creditsByPlayer.remove(playerId);
        }
        return previous;
    }

    public synchronized int clearKit(UUID playerId, String kitName) {
        int previous = get(playerId, kitName);
        set(playerId, kitName, 0);
        return previous;
    }

    public synchronized boolean isEmpty() {
        return creditsByPlayer.isEmpty();
    }

    synchronized Map<UUID, Map<String, Integer>> snapshot() {
        Map<UUID, Map<String, Integer>> copy = new HashMap<>();
        for (Map.Entry<UUID, Map<String, Integer>> entry : creditsByPlayer.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
            }
        }
        return copy;
    }

    synchronized void replaceAll(Map<UUID, Map<String, Integer>> loaded) {
        creditsByPlayer.clear();
        if (loaded == null) {
            return;
        }
        for (Map.Entry<UUID, Map<String, Integer>> entry : loaded.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            Map<String, Integer> kits = new HashMap<>();
            for (Map.Entry<String, Integer> kit : entry.getValue().entrySet()) {
                String key = normalizeKitName(kit.getKey());
                if (key == null || kit.getValue() == null || kit.getValue() <= 0) {
                    continue;
                }
                kits.put(key, kit.getValue());
            }
            if (!kits.isEmpty()) {
                creditsByPlayer.put(entry.getKey(), kits);
            }
        }
    }

    public static String normalizeKitName(String kitName) {
        if (kitName == null) {
            return null;
        }
        String normalized = kitName.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
