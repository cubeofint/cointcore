package com.mawlee.cointcore.mixin.adastra;

import com.mawlee.cointcore.adastra.AdAstraDistributionCache;
import earth.terrarium.adastra.api.systems.OxygenApi;
import earth.terrarium.adastra.api.systems.TemperatureApi;
import earth.terrarium.adastra.common.blockentities.base.MachineBlockEntity;
import earth.terrarium.adastra.common.blockentities.machines.OxygenDistributorBlockEntity;
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
 * A sealed oxygen volume is scanned again only after a block on its interior
 * or wall changes. The scan writes oxygen and temperature only for positions
 * that appeared or disappeared, and machines share the refresh window.
 * Energy and oxygen consumption in {@code serverTick} stay every tick.
 */
@Mixin(value = OxygenDistributorBlockEntity.class, remap = false)
public abstract class AdAstraOxygenDistributorMixin {
    private static final short ROOM_TEMPERATURE = 22;

    @Shadow
    @Final
    private Set<BlockPos> lastDistributedBlocks;

    @Shadow
    private boolean shouldSyncPositions;

    @Shadow
    protected abstract void tickOxygen(ServerLevel level, BlockPos pos, BlockState state);

    private boolean cointcore$distributing;

    @Redirect(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Learth/terrarium/adastra/common/blockentities/machines/OxygenDistributorBlockEntity;tickOxygen(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
            )
    )
    private void cointcore$tickOxygen(OxygenDistributorBlockEntity self, ServerLevel level, BlockPos pos, BlockState state) {
        if (AdAstraDistributionCache.isStable(self)) {
            return;
        }
        if (AdAstraDistributionCache.phase(pos) != 0) {
            AdAstraDistributionCache.arm(self);
            return;
        }
        tickOxygen(level, pos, state);
        AdAstraDistributionCache.snapshot(self, level, lastDistributedBlocks);
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private void cointcore$resetOxygenRun(CallbackInfo ci) {
        cointcore$distributing = false;
    }

    @Redirect(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Learth/terrarium/adastra/common/blockentities/machines/OxygenDistributorBlockEntity;setLit(Z)V"
            )
    )
    private void cointcore$watchOxygenLit(OxygenDistributorBlockEntity self, boolean lit) {
        cointcore$distributing = lit;
        ((MachineBlockEntity) (Object) this).setLit(lit);
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private void cointcore$staggeredOxygen(ServerLevel level, long gameTime, BlockState state, BlockPos pos, CallbackInfo ci) {
        OxygenDistributorBlockEntity self = (OxygenDistributorBlockEntity) (Object) this;
        if (!cointcore$distributing || !AdAstraDistributionCache.isDue(self, pos, gameTime)) {
            return;
        }
        AdAstraDistributionCache.disarm(self);
        tickOxygen(level, pos, state);
        AdAstraDistributionCache.snapshot(self, level, lastDistributedBlocks);
    }

    @Redirect(
            method = "tickOxygen",
            at = @At(
                    value = "INVOKE",
                    target = "Learth/terrarium/adastra/api/systems/OxygenApi;setOxygen(Lnet/minecraft/world/level/Level;Ljava/util/Collection;Z)V"
            )
    )
    private void cointcore$deferOxygenWrite(OxygenApi api, Level level, Collection<BlockPos> positions, boolean oxygen) {
    }

    @Redirect(
            method = "tickOxygen",
            at = @At(
                    value = "INVOKE",
                    target = "Learth/terrarium/adastra/api/systems/TemperatureApi;setTemperature(Lnet/minecraft/world/level/Level;Ljava/util/Collection;S)V"
            )
    )
    private void cointcore$deferTemperatureWrite(TemperatureApi api, Level level, Collection<BlockPos> positions, short temperature) {
    }

    @Inject(method = "resetLastDistributedBlocks", at = @At("HEAD"), cancellable = true)
    private void cointcore$deltaOxygen(Set<BlockPos> next, CallbackInfo ci) {
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
            OxygenApi.API.removeOxygen(level, removed);
            TemperatureApi.API.removeTemperature(level, removed);
        }
        if (level != null && !added.isEmpty()) {
            OxygenApi.API.setOxygen(level, added, true);
            TemperatureApi.API.setTemperature(level, added, ROOM_TEMPERATURE);
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

    @Inject(method = "clearOxygenBlocks", at = @At("HEAD"))
    private void cointcore$dropOxygenCache(CallbackInfo ci) {
        AdAstraDistributionCache.invalidate((BlockEntity) (Object) this);
    }

    @Inject(method = "onRemoved", at = @At("HEAD"))
    private void cointcore$dropOxygenOnRemove(CallbackInfo ci) {
        AdAstraDistributionCache.invalidate((BlockEntity) (Object) this);
    }
}
