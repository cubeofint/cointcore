package com.mawlee.cointcore.mixin.industrialforegoingsouls;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.config.SoulSurgePerfConfig;
import com.mawlee.cointcore.industrialforegoingsouls.SoulSurgeTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.List;

/**
 * Soul Surge accelerates only the single attached block entity (facing.opposite)
 * at the mod's {@code ACCELERATION_TICK}. No deny lists, no chunk-limit gate,
 * no entity / random-tick / neighbour scans.
 */
@Mixin(targets = "com.buuz135.industrialforegoingsouls.block.tile.SoulSurgeBlockEntity", remap = false)
public abstract class SoulSurgeBlockEntityMixin {
    private static final String SERVER_TICK =
            "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lcom/buuz135/industrialforegoingsouls/block/tile/SoulSurgeBlockEntity;)V";

    /**
     * IF Souls reads the target via {@code level.getBlockState(attached)}.
     * Invalid targets (not the attached cell, air, no BE, another surge) become
     * air so the vanilla method skips BE ticks, random ticks, and soul spend.
     */
    @WrapOperation(
            method = SERVER_TICK,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                    remap = true
            ),
            remap = false
    )
    private BlockState cointcore$restrictTargetState(
            Level level,
            BlockPos queryPos,
            Operation<BlockState> original
    ) {
        BlockState queried = original.call(level, queryPos);
        if (!SoulSurgePerfConfig.isEnabled()) {
            return queried;
        }
        if (!queryPos.equals(cointcore$attachedPos())) {
            return Blocks.AIR.defaultBlockState();
        }
        if (!SoulSurgeTarget.shouldAccelerate(
                queried.isAir(),
                level.getBlockEntity(queryPos) != null,
                cointcore$blockId(queried)
        )) {
            return Blocks.AIR.defaultBlockState();
        }
        return queried;
    }

    /**
     * Bypass IF Souls {@code cant_accelerate} / forge tick-acceleration tags.
     * Shared {@code TickAccelerationDeny} is intentionally not consulted.
     */
    @WrapOperation(
            method = SERVER_TICK,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z",
                    remap = true
            ),
            remap = false
    )
    private boolean cointcore$bypassCantAccelerateTags(BlockState state, TagKey<?> tag, Operation<Boolean> original) {
        if (SoulSurgePerfConfig.isEnabled()) {
            return false;
        }
        return original.call(state, tag);
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
        if (!targetPos.equals(cointcore$attachedPos())) {
            return;
        }
        if (!SoulSurgeTarget.shouldAccelerate(
                targetState.isAir(),
                blockEntity != null,
                cointcore$blockId(targetState)
        )) {
            return;
        }
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
    private void cointcore$skipRandomTickAcceleration(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            RandomSource random,
            Operation<Void> original
    ) {
        if (SoulSurgePerfConfig.isEnabled()) {
            return;
        }
        original.call(state, level, pos, random);
    }

    @WrapOperation(
            method = SERVER_TICK,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
                    remap = true
            ),
            remap = false
    )
    private <T extends Entity> List<T> cointcore$skipEntityAccelerationScan(
            Level level,
            Class<T> entityClass,
            AABB aabb,
            Operation<List<T>> original
    ) {
        if (SoulSurgePerfConfig.isEnabled()) {
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
    private void cointcore$skipEntityAccelerationTick(LivingEntity entity, Operation<Void> original) {
        if (SoulSurgePerfConfig.isEnabled()) {
            return;
        }
        original.call(entity);
    }

    @Unique
    private BlockPos cointcore$attachedPos() {
        BlockEntity surge = (BlockEntity) (Object) this;
        SoulSurgeTarget.Offset offset = SoulSurgeTarget.attachedOffset(cointcore$facingName(surge.getBlockState()));
        return surge.getBlockPos().offset(offset.x(), offset.y(), offset.z());
    }

    @Unique
    private static String cointcore$facingName(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING).getSerializedName();
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getSerializedName();
        }
        return null;
    }

    @Unique
    private static String cointcore$blockId(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null ? id.toString() : "";
    }
}
