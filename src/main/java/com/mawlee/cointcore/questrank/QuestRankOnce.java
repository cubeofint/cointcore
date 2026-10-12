package com.mawlee.cointcore.questrank;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * First-time flag per key, used so quest-rank failures WARN once.
 */
public final class QuestRankOnce {
    private static final ConcurrentHashMap<String, Boolean> KEYS = new ConcurrentHashMap<>();

    private QuestRankOnce() {
    }

    public static boolean first(String key) {
        return KEYS.putIfAbsent(normalize(key), Boolean.TRUE) == null;
    }

    public static boolean already(String key) {
        return KEYS.containsKey(normalize(key));
    }

    public static <T> T guard(String key, T fallback, Supplier<T> action) {
        try {
            return action.get();
        } catch (Throwable ignored) {
            first(key);
            return fallback;
        }
    }

    public static void reset() {
        KEYS.clear();
    }

    private static String normalize(String key) {
        return key == null || key.isBlank() ? "unknown" : key;
    }
}
