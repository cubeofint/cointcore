package com.mawlee.cointcore.spawner;

import com.mawlee.cointcore.config.SpawnerLootConfig;

/**
 * Marks the current thread as converting a machine spawn into virtual loot so
 * {@code EventHooks.finalizeMobSpawn} can be skipped (Apotheosis elites / affix
 * gear / Aether accessories). Cheap affix drops are rolled separately.
 */
public final class VirtualSpawnContext {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private VirtualSpawnContext() {
    }

    public static void enter() {
        if (!SpawnerLootConfig.skipFinalizeMobSpawn()) {
            return;
        }
        DEPTH.set(DEPTH.get() + 1);
    }

    public static void exit() {
        int depth = DEPTH.get();
        if (depth <= 1) {
            DEPTH.remove();
        } else {
            DEPTH.set(depth - 1);
        }
    }

    public static boolean skipFinalize() {
        return DEPTH.get() > 0 && SpawnerLootConfig.skipFinalizeMobSpawn();
    }
}
