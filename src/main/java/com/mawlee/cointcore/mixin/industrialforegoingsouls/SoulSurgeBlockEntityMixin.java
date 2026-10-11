package com.mawlee.cointcore.mixin.industrialforegoingsouls;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.config.SoulSurgePerfConfig;
import com.mawlee.cointcore.industrialforegoingsouls.SoulSurgeAcceleration;
import com.mawlee.cointcore.industrialforegoingsouls.SoulSurgeAccelerationClaim;
import com.mawlee.cointcore.industrialforegoingsouls.SoulSurgeClaimTracker;
import com.mawlee.cointcore.industrialforegoingsouls.SoulSurgeFacing;
import com.mawlee.cointcore.industrialforegoingsouls.SoulSurgeSouls;
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
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

/**
 * Soul Surge accelerates only the single attached block entity
 * ({@code pos.relative(facing.opposite)}) at the mod's {@code ACCELERATION_TICK}.
 * Facing is read by property name {@code facing} (Titanium
 * {@code RotatableBlock.FACING_ALL}), not {@code BlockStateProperties} identity.
 * A target may be claimed by at most one surge per game tick (sticky owner),
 * and only after IF Souls 1.10.7 has {@code tickingTime > 0} (soul already
 * paid or just consumed from the pipe network). A surge that fails to
 * accelerate releases immediately.
 * No deny lists, no chunk-limit gate, no entity / random-tick / neighbour scans.
 */
@Mixin(targets = "com.buuz135.industrialforegoingsouls.block.tile.SoulSurgeBlockEntity", remap = false)
public abstract class SoulSurgeBlockEntityMixin {
    private static final String SERVER_TICK =
            "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lcom/buuz135/industrialforegoingsouls/block/tile/SoulSurgeBlockEntity;)V";

    /**
     * Leftover paid soul duration. IF Souls refills this from the network
     * before {@code getBlockState} / ticker calls when it was {@code <= 0}.
     */
    @Shadow(remap = false)
    private int tickingTime;

    @Unique
    private long cointcore$claimedTargetPacked;

    @Unique
    private boolean cointcore$hasClaim;

    @Unique
    private boolean cointcore$acceleratedThisTick;

    /**
     * IF Souls reads the target via {@code level.getBlockState(attached)}.
     * Invalid targets (not the attached cell, air, no BE, another surge) and
     * surges that lost the per-tick claim become air so the vanilla method
     * skips BE ticks, random ticks, and soul spend.
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
            cointcore$releaseClaim(level);
            return Blocks.AIR.defaultBlockState();
        }
        // After IF Souls soul refill: only tickingTime > 0 may reserve the target.
        if (!cointcore$tryAccelerate(level, queryPos, this.tickingTime > 0)) {
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
            cointcore$releaseClaim(level);
            return;
        }
        if (!cointcore$tryAccelerate(level, targetPos, this.tickingTime > 0)) {
            return;
        }
        cointcore$acceleratedThisTick = true;
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

    @Inject(method = SERVER_TICK, at = @At("HEAD"))
    private void cointcore$beginTick(CallbackInfo ci) {
        cointcore$acceleratedThisTick = false;
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level == null || !SoulSurgePerfConfig.isEnabled()) {
            return;
        }
        if (!SoulSurgeSouls.hasSouls(this)) {
            cointcore$releaseClaim(level);
        }
    }

    @Inject(method = SERVER_TICK, at = @At("RETURN"))
    private void cointcore$endTick(CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level == null || !SoulSurgePerfConfig.isEnabled()) {
            return;
        }
        if (!cointcore$acceleratedThisTick) {
            cointcore$releaseClaim(level);
        }
    }

    @Inject(method = "setRemoved()V", at = @At("HEAD"), require = 0)
    private void cointcore$releaseClaimOnRemoved(CallbackInfo ci) {
        BlockEntity surge = (BlockEntity) (Object) this;
        Level level = surge.getLevel();
        if (level != null && SoulSurgePerfConfig.isEnabled()) {
            cointcore$releaseClaim(level);
        }
    }

    @Inject(method = "onChunkUnloaded()V", at = @At("HEAD"), require = 0)
    private void cointcore$releaseClaimOnUnload(CallbackInfo ci) {
        BlockEntity surge = (BlockEntity) (Object) this;
        Level level = surge.getLevel();
        if (level != null && SoulSurgePerfConfig.isEnabled()) {
            cointcore$releaseClaim(level);
        }
    }

    @Unique
    private boolean cointcore$tryAccelerate(Level level, BlockPos targetPos, boolean canWorkThisTick) {
        long targetPacked = SoulSurgeAccelerationClaim.pack(targetPos.getX(), targetPos.getY(), targetPos.getZ());
        long surgePacked = cointcore$selfPacked();
        SoulSurgeAccelerationClaim claims = cointcore$claimsFor(level);
        long gameTime = level.getGameTime();
        if (cointcore$hasClaim && cointcore$claimedTargetPacked != targetPacked) {
            claims.release(cointcore$claimedTargetPacked, surgePacked, gameTime);
            cointcore$hasClaim = false;
        }
        boolean claimed = SoulSurgeAcceleration.tryAccelerate(
                claims, targetPacked, surgePacked, gameTime, canWorkThisTick
        );
        cointcore$hasClaim = claimed;
        if (claimed) {
            cointcore$claimedTargetPacked = targetPacked;
        }
        return claimed;
    }

    @Unique
    private void cointcore$releaseClaim(Level level) {
        long surgePacked = cointcore$selfPacked();
        long gameTime = level.getGameTime();
        SoulSurgeAccelerationClaim claims = cointcore$claimsFor(level);
        BlockPos attached = cointcore$attachedPos();
        claims.release(
                SoulSurgeAccelerationClaim.pack(attached.getX(), attached.getY(), attached.getZ()),
                surgePacked,
                gameTime
        );
        if (cointcore$hasClaim) {
            claims.release(cointcore$claimedTargetPacked, surgePacked, gameTime);
        }
        cointcore$hasClaim = false;
    }

    @Unique
    private static SoulSurgeAccelerationClaim cointcore$claimsFor(Level level) {
        return SoulSurgeClaimTracker.forLevel(level);
    }

    @Unique
    private long cointcore$selfPacked() {
        BlockPos pos = ((BlockEntity) (Object) this).getBlockPos();
        return SoulSurgeAccelerationClaim.pack(pos.getX(), pos.getY(), pos.getZ());
    }

    @Unique
    private BlockPos cointcore$attachedPos() {
        BlockEntity surge = (BlockEntity) (Object) this;
        return SoulSurgeFacing.target(surge.getBlockPos(), surge.getBlockState());
    }

    @Unique
    private static String cointcore$blockId(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null ? id.toString() : "";
    }
}
