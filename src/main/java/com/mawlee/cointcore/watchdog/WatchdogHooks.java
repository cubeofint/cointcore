package com.mawlee.cointcore.watchdog;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Thin hooks used by mixins. Keep work here so mixin classes stay tiny.
 *
 * <p>Server-thread only: start timestamps are plain longs (no ThreadLocal boxing).
 * {@code System.nanoTime} around BE/entity runs only when the matching hot flag is set.
 */
public final class WatchdogHooks {
    private static long blockEntityStartNanos;
    private static long entityStartNanos;
    private static long serverTickStartNanos;

    private WatchdogHooks() {
    }

    public static void onServerTickStart() {
        TickWatchdogService.instance().onServerTickStart();
        if (!WatchdogHotFlags.RUNNING) {
            return;
        }
        serverTickStartNanos = System.nanoTime();
        TickWatchdogService.instance().sampler().onTickStart(Thread.currentThread());
    }

    public static void onServerTickEnd(net.minecraft.server.MinecraftServer server) {
        TickWatchdogService.instance().sampler().onTickEnd();
        long started = serverTickStartNanos;
        serverTickStartNanos = 0L;
        long nanos = started == 0L ? 0L : System.nanoTime() - started;
        TickWatchdogService.instance().onServerTickEnd(server, nanos);
        TickProbe.clear();
    }

    public static void beforeBlockEntityTick(BlockEntity blockEntity) {
        if (!WatchdogHotFlags.TIME_BLOCK_ENTITIES || blockEntity == null) {
            return;
        }
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        blockEntityStartNanos = System.nanoTime();
        if (StackSampler.isCapturing()) {
            BlockPos pos = blockEntity.getBlockPos();
            TickProbe.enter(
                    "block_entity",
                    blockEntity.getType(),
                    level.dimension(),
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()
            );
        }
    }

    public static void afterBlockEntityTick(BlockEntity blockEntity) {
        if (!WatchdogHotFlags.TIME_BLOCK_ENTITIES || blockEntity == null) {
            TickProbe.exit();
            return;
        }
        long started = blockEntityStartNanos;
        blockEntityStartNanos = 0L;
        TickProbe.exit();
        if (started == 0L) {
            return;
        }
        long nanos = System.nanoTime() - started;
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        TickWatchdogService.instance().recordBlockEntity(
                blockEntity.getType(),
                level.dimension(),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                nanos
        );
    }

    public static void beforeEntityTick(Entity entity) {
        if (!WatchdogHotFlags.TIME_ENTITIES || entity == null || entity.level().isClientSide()) {
            return;
        }
        entityStartNanos = System.nanoTime();
        if (StackSampler.isCapturing()) {
            TickProbe.enter(
                    "entity",
                    entity.getType(),
                    entity.level().dimension(),
                    entity.getBlockX(),
                    entity.getBlockY(),
                    entity.getBlockZ()
            );
        }
    }

    public static void afterEntityTick(Entity entity) {
        if (!WatchdogHotFlags.TIME_ENTITIES || entity == null) {
            TickProbe.exit();
            return;
        }
        long started = entityStartNanos;
        entityStartNanos = 0L;
        TickProbe.exit();
        if (started == 0L) {
            return;
        }
        long nanos = System.nanoTime() - started;
        if (entity.level().isClientSide()) {
            return;
        }
        TickWatchdogService.instance().recordEntity(
                entity.getType(),
                entity.level().dimension(),
                entity.getBlockX(),
                entity.getBlockY(),
                entity.getBlockZ(),
                nanos
        );
    }
}
