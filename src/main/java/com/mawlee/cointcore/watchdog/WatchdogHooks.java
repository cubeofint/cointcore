package com.mawlee.cointcore.watchdog;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Thin hooks used by mixins. Keep work here so mixin classes stay tiny.
 */
public final class WatchdogHooks {
    private static final ThreadLocal<Long> BLOCK_ENTITY_START = ThreadLocal.withInitial(() -> 0L);
    private static final ThreadLocal<Long> ENTITY_START = ThreadLocal.withInitial(() -> 0L);
    private static final ThreadLocal<Long> SERVER_TICK_START = ThreadLocal.withInitial(() -> 0L);

    private WatchdogHooks() {
    }

    public static void onServerTickStart() {
        TickWatchdogService.instance().onServerTickStart();
        SERVER_TICK_START.set(System.nanoTime());
        if (TickWatchdogService.instance().isRunning()) {
            TickWatchdogService.instance().sampler().onTickStart(Thread.currentThread());
        }
    }

    public static void onServerTickEnd(net.minecraft.server.MinecraftServer server) {
        TickWatchdogService.instance().sampler().onTickEnd();
        long started = SERVER_TICK_START.get();
        long nanos = started == 0L ? 0L : System.nanoTime() - started;
        SERVER_TICK_START.set(0L);
        TickWatchdogService.instance().onServerTickEnd(server, nanos);
        TickProbe.clear();
    }

    public static void beforeBlockEntityTick(BlockEntity blockEntity) {
        if (!TickWatchdogService.isTimingEnabled() || blockEntity == null) {
            return;
        }
        Level level = blockEntity.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        String typeId = typeId(blockEntity);
        String dim = WatchdogWorldContext.dimensionId(serverLevel);
        String mod = WatchdogWorldContext.owningMod(typeId);
        TickProbe.enterBlockEntity(typeId, mod, dim, pos.getX(), pos.getY(), pos.getZ());
        BLOCK_ENTITY_START.set(System.nanoTime());
    }

    public static void afterBlockEntityTick(BlockEntity blockEntity) {
        if (!TickWatchdogService.isTimingEnabled() || blockEntity == null) {
            TickProbe.exit();
            return;
        }
        long started = BLOCK_ENTITY_START.get();
        BLOCK_ENTITY_START.set(0L);
        TickProbe.exit();
        if (started == 0L) {
            return;
        }
        long nanos = System.nanoTime() - started;
        Level level = blockEntity.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        String typeId = typeId(blockEntity);
        TickWatchdogService.instance().recordBlockEntity(new TickWatchdogService.BlockEntityTiming(
                typeId,
                WatchdogWorldContext.owningMod(typeId),
                WatchdogWorldContext.dimensionId(serverLevel),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                nanos
        ));
    }

    public static void beforeEntityTick(Entity entity) {
        if (!TickWatchdogService.isTimingEnabled() || entity == null || entity.level().isClientSide()) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        String typeId = WatchdogWorldContext.entityTypeId(entity);
        String dim = WatchdogWorldContext.dimensionId(serverLevel);
        String mod = WatchdogWorldContext.owningMod(typeId);
        BlockPos pos = entity.blockPosition();
        TickProbe.enterEntity(typeId, mod, dim, pos.getX(), pos.getY(), pos.getZ());
        ENTITY_START.set(System.nanoTime());
    }

    public static void afterEntityTick(Entity entity) {
        if (!TickWatchdogService.isTimingEnabled() || entity == null) {
            TickProbe.exit();
            return;
        }
        long started = ENTITY_START.get();
        ENTITY_START.set(0L);
        TickProbe.exit();
        if (started == 0L) {
            return;
        }
        long nanos = System.nanoTime() - started;
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        String typeId = WatchdogWorldContext.entityTypeId(entity);
        BlockPos pos = entity.blockPosition();
        TickWatchdogService.instance().recordEntity(new TickWatchdogService.EntityTiming(
                typeId,
                WatchdogWorldContext.owningMod(typeId),
                WatchdogWorldContext.dimensionId(serverLevel),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                nanos
        ));
    }

    private static String typeId(BlockEntity blockEntity) {
        ResourceLocation key = BlockEntityType.getKey(blockEntity.getType());
        return key == null ? "unknown" : key.toString();
    }
}
