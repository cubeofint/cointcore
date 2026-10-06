package com.mawlee.cointcore.mixin.industrialforegoingsouls;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.chunklimit.ChunkLimitService;
import com.mawlee.cointcore.config.ChunkLimitConfig;
import com.mawlee.cointcore.config.SoulSurgePerfConfig;
import com.mawlee.cointcore.tickaccel.TickAccelerationDeny;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

/**
 * Caps Soul Surge neighbour acceleration, respects chunk-limits soft-disable, applies deny list,
 * and gates entity / randomTick acceleration paths.
 */
@Mixin(targets = "com.buuz135.industrialforegoingsouls.block.tile.SoulSurgeBlockEntity", remap = false)
public abstract class SoulSurgeBlockEntityMixin {
    @Unique
    private static final ThreadLocal<Integer> COINTCORE$ACCEL_COUNT = ThreadLocal.withInitial(() -> 0);

    private static final String SERVER_TICK =
            "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lcom/buuz135/industrialforegoingsouls/block/tile/SoulSurgeBlockEntity;)V";

    @Inject(method = SERVER_TICK, at = @At("HEAD"), remap = false)
    private void cointcore$resetAccelCount(CallbackInfo ci) {
        COINTCORE$ACCEL_COUNT.set(0);
    }

    @WrapOperation(
            method = SERVER_TICK,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
                    remap = true
            ),
            remap = false
    )
    private void cointcore$gateAcceleratedTick(
            BlockEntityTicker<?> ticker,
            Level level,
            BlockPos targetPos,
            BlockState targetState,
            BlockEntity blockEntity,
            Operation<Void> original
    ) {
        if (!SoulSurgePerfConfig.isEnabled()) {
            original.call(ticker, level, targetPos, targetState, blockEntity);
            return;
        }

        if (SoulSurgePerfConfig.isDenied(targetState) || TickAccelerationDeny.isDenied(targetState)) {
            return;
        }

        if (SoulSurgePerfConfig.respectChunkLimits()
                && ChunkLimitConfig.isEnabled()
                && ChunkLimitConfig.hasAnyBlockLimits()
                && level instanceof ServerLevel serverLevel
                && !ChunkLimitService.shouldTickLimitedBlock(serverLevel, targetPos, targetState.getBlock())) {
            return;
        }

        int max = SoulSurgePerfConfig.getMaxAccelerationTicks();
        if (max <= 0) {
            return;
        }

        int used = COINTCORE$ACCEL_COUNT.get();
        if (used >= max) {
            return;
        }
        COINTCORE$ACCEL_COUNT.set(used + 1);
        original.call(ticker, level, targetPos, targetState, blockEntity);
    }

    @WrapOperation(
            method = SERVER_TICK,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
                    remap = true
            ),
            remap = false
    )
    private void cointcore$gateRandomTickAcceleration(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            Operation<Void> original
    ) {
        if (SoulSurgePerfConfig.isEnabled() && !SoulSurgePerfConfig.allowBlockRandomTickAcceleration()) {
            return;
        }
        if (SoulSurgePerfConfig.isEnabled()
                && (SoulSurgePerfConfig.isDenied(state) || TickAccelerationDeny.isDenied(state))) {
            return;
        }
        original.call(state, level, pos, random);
    }

    /**
     * Skip AABB entity scan entirely when entity acceleration is disabled.
     */
    @WrapOperation(
            method = SERVER_TICK,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
                    remap = true
            ),
            remap = false
    )
    private <T extends Entity> List<T> cointcore$gateEntityAccelerationScan(
            Level level,
            Class<T> entityClass,
            AABB aabb,
            Operation<List<T>> original
    ) {
        if (SoulSurgePerfConfig.isEnabled() && !SoulSurgePerfConfig.allowEntityAcceleration()) {
            return Collections.emptyList();
        }
        return original.call(level, entityClass, aabb);
    }

    @WrapOperation(
            method = SERVER_TICK,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;tick()V",
                    remap = true
            ),
            remap = false
    )
    private void cointcore$gateEntityAccelerationTick(LivingEntity entity, Operation<Void> original) {
        if (SoulSurgePerfConfig.isEnabled() && !SoulSurgePerfConfig.allowEntityAcceleration()) {
            return;
        }
        original.call(entity);
    }
}
