package com.mawlee.cointcore.spawner;

import com.mawlee.cointcore.config.SpawnerByproductConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;

/**
 * Turns a machine spawn into the same adjacent-inventory loot roll used by farm spawners.
 * The mob is never added to the world. Overflow is voided.
 */
public final class MachineSpawnLoot {
    private static final ThreadLocal<ArrayDeque<BlockPos>> ORIGIN = ThreadLocal.withInitial(ArrayDeque::new);

    private MachineSpawnLoot() {
    }

    public static void pushOrigin(BlockPos pos) {
        ORIGIN.get().push(pos.immutable());
    }

    public static void popOrigin() {
        ArrayDeque<BlockPos> stack = ORIGIN.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public static boolean captureFresh(Level level, Entity entity) {
        return capture(level, entity, peekOrigin(), false);
    }

    public static boolean captureFresh(Level level, Entity entity, BlockPos origin) {
        return capture(level, entity, origin, false);
    }

    public static boolean capturePassengers(ServerLevel level, Entity entity) {
        return capture(level, entity, peekOrigin(), true);
    }

    public static boolean capturePassengers(ServerLevel level, Entity entity, BlockPos origin) {
        return capture(level, entity, origin, true);
    }

    private static BlockPos peekOrigin() {
        ArrayDeque<BlockPos> stack = ORIGIN.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    private static boolean capture(Level level, Entity entity, BlockPos origin, boolean passengers) {
        if (!(entity instanceof Mob mob) || !(level instanceof ServerLevel server)) {
            return addOriginal(level, entity, passengers);
        }

        BlockPos pos = origin != null ? origin : mob.blockPosition();
        try {
            boolean itemSink = SpawnerLootService.hasAdjacentStorageWithSpace(server, pos);
            boolean fluidSink = SpawnerByproductService.hasFluidByproductSink(server, pos);
            if (itemSink) {
                SpawnerLootService.generateAndInsert(server, pos, mob);
            }
            if (fluidSink || (SpawnerByproductConfig.isEnabled() && itemSink)) {
                SpawnerByproductService.tryDepositAll(server, pos, mob);
            }
        } finally {
            mob.discard();
        }
        return true;
    }

    private static boolean addOriginal(Level level, Entity entity, boolean passengers) {
        if (passengers && level instanceof ServerLevel server) {
            return server.tryAddFreshEntityWithPassengers(entity);
        }
        return level.addFreshEntity(entity);
    }
}
