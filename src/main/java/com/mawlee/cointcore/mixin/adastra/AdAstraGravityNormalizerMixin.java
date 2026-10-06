package com.mawlee.cointcore.mixin.adastra;

import com.mawlee.cointcore.adastra.AdAstraDistributionCache;
import earth.terrarium.adastra.api.systems.GravityApi;
import earth.terrarium.adastra.common.blockentities.base.MachineBlockEntity;
import earth.terrarium.adastra.common.blockentities.machines.GravityNormalizerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Same sealed-volume skip as the oxygen distributor. A rescan writes gravity
 * only for positions that appeared or disappeared. Energy use stays every tick.
 */
@Mixin(value = GravityNormalizerBlockEntity.class, remap = false)
public abstract class AdAstraGravityNormalizerMixin {
    @Shadow
    @Final
    private Set<BlockPos> lastDistributedBlocks;

    @Shadow
    private float targetGravity;

    @Shadow
    private boolean shouldSyncPositions;

    @Shadow
    protected abstract void tickGravity(ServerLevel level, BlockPos pos, BlockState state);

    private boolean cointcore$distributing;

    @Redirect(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Learth/terrarium/adastra/common/blockentities/machines/GravityNormalizerBlockEntity;tickGravity(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
            )
    )
    private void cointcore$tickGravity(GravityNormalizerBlockEntity self, ServerLevel level, BlockPos pos, BlockState state) {
        if (AdAstraDistributionCache.isStable(self)) {
            return;
        }
        if (AdAstraDistributionCache.phase(pos) != 0) {
            AdAstraDistributionCache.arm(self);
            return;
        }
        tickGravity(level, pos, state);
        AdAstraDistributionCache.snapshot(self, level, lastDistributedBlocks);
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private void cointcore$resetGravityRun(CallbackInfo ci) {
        cointcore$distributing = false;
    }

    @Redirect(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Learth/terrarium/adastra/common/blockentities/machines/GravityNormalizerBlockEntity;setLit(Z)V"
            )
    )
    private void cointcore$watchGravityLit(GravityNormalizerBlockEntity self, boolean lit) {
        cointcore$distributing = lit;
        ((MachineBlockEntity) (Object) this).setLit(lit);
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private void cointcore$staggeredGravity(ServerLevel level, long gameTime, BlockState state, BlockPos pos, CallbackInfo ci) {
        GravityNormalizerBlockEntity self = (GravityNormalizerBlockEntity) (Object) this;
        if (!cointcore$distributing || !AdAstraDistributionCache.isDue(self, pos, gameTime)) {
            return;
        }
        AdAstraDistributionCache.disarm(self);
        tickGravity(level, pos, state);
        AdAstraDistributionCache.snapshot(self, level, lastDistributedBlocks);
    }

    @Redirect(
            method = "tickGravity",
            at = @At(
                    value = "INVOKE",
                    target = "Learth/terrarium/adastra/api/systems/GravityApi;setGravity(Lnet/minecraft/world/level/Level;Ljava/util/Collection;F)V"
            )
    )
    private void cointcore$deferGravityWrite(GravityApi api, Level level, Collection<BlockPos> positions, float gravity) {
    }

    @Inject(method = "resetLastDistributedBlocks", at = @At("HEAD"), cancellable = true)
    private void cointcore$deltaGravity(Set<BlockPos> next, CallbackInfo ci) {
        List<BlockPos> removed = new ArrayList<>();
        for (BlockPos pos : lastDistributedBlocks) {
            if (!next.contains(pos)) {
                removed.add(pos);
            }
        }
        List<BlockPos> added = new ArrayList<>();
        for (BlockPos pos : next) {
            if (!lastDistributedBlocks.contains(pos)) {
                added.add(pos);
            }
        }
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && !removed.isEmpty()) {
            GravityApi.API.removeGravity(level, removed);
        }
        if (level != null && !added.isEmpty()) {
            GravityApi.API.setGravity(level, added, targetGravity);
        }
        if (removed.isEmpty() && added.isEmpty()) {
            ci.cancel();
            return;
        }
        lastDistributedBlocks.clear();
        lastDistributedBlocks.addAll(next);
        shouldSyncPositions = true;
        ((MachineBlockEntity) (Object) this).sync();
        ci.cancel();
    }

    @Inject(method = "clearGravityBlocks", at = @At("HEAD"))
    private void cointcore$dropGravityCache(CallbackInfo ci) {
        AdAstraDistributionCache.invalidate((BlockEntity) (Object) this);
    }

    @Inject(method = "onRemoved", at = @At("HEAD"))
    private void cointcore$dropGravityOnRemove(CallbackInfo ci) {
        AdAstraDistributionCache.invalidate((BlockEntity) (Object) this);
    }
}
