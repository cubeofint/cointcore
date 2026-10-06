package com.mawlee.cointcore.adastra;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Applies every live air vortex in one entity walk per dimension.
 * Each vortex still uses its own impulse.
 */
public final class AdAstraVortexPass {
    private static final Map<ServerLevel, List<Consumer<Entity>>> PENDING = new IdentityHashMap<>();

    private AdAstraVortexPass() {
    }

    public static void join(ServerLevel level, Consumer<Entity> consumer) {
        PENDING.computeIfAbsent(level, ignored -> new ArrayList<>(1)).add(consumer);
    }

    public static void flush() {
        if (PENDING.isEmpty()) {
            return;
        }
        for (Map.Entry<ServerLevel, List<Consumer<Entity>>> entry : PENDING.entrySet()) {
            ServerLevel level = entry.getKey();
            List<Consumer<Entity>> consumers = entry.getValue();
            for (Entity entity : level.getAllEntities()) {
                for (int i = 0; i < consumers.size(); i++) {
                    consumers.get(i).accept(entity);
                }
            }
        }
        PENDING.clear();
    }
}
