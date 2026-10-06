package com.mawlee.cointcore.mixin.adastra;

import com.mawlee.cointcore.adastra.AdAstraPipeNetworks;
import earth.terrarium.adastra.common.blockentities.pipes.Pipe;
import earth.terrarium.adastra.common.blockentities.pipes.PipeBlockEntity;
import earth.terrarium.adastra.common.config.MachineConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Controllers still transfer every tick from the last source and consumer
 * maps. Those maps are rebuilt after a pipe or neighbor change, including on
 * the ticks between Ad Astra's shared refresh, and are left in place when the
 * network has not changed.
 */
@Mixin(value = PipeBlockEntity.class, remap = false)
public abstract class AdAstraPipeBlockEntityMixin {
    @Accessor("sources")
    abstract Map<BlockPos, Direction> cointcore$sources();

    @Accessor("consumers")
    abstract Map<BlockPos, Direction> cointcore$consumers();

    @Accessor("isController")
    abstract boolean cointcore$isController();

    @Inject(method = "pipeChanged", at = @At("RETURN"))
    private void cointcore$markNetworkDirty(Level level, BlockPos pos, CallbackInfo ci) {
        AdAstraPipeNetworks.markNetworkDirty(level, pos);
    }

    @Redirect(
            method = "serverTick",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;clear()V")
    )
    private void cointcore$clearWhenRebuilding(Map<BlockPos, Direction> map) {
        if (AdAstraPipeNetworks.needsRebuild((PipeBlockEntity) (Object) this)) {
            map.clear();
        }
    }

    @Redirect(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Learth/terrarium/adastra/common/blockentities/pipes/PipeBlockEntity;findNodes(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V"
            )
    )
    private void cointcore$findNodes(PipeBlockEntity self, ServerLevel level, BlockPos pos) {
        if (!AdAstraPipeNetworks.needsRebuild(self)) {
            return;
        }
        ((Pipe) self).findNodes(level, pos);
        AdAstraPipeNetworks.markClean(self);
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private void cointcore$rebuildWhenDirty(
            ServerLevel level,
            long gameTime,
            BlockState state,
            BlockPos pos,
            CallbackInfo ci
    ) {
        if (!cointcore$isController()) {
            return;
        }
        PipeBlockEntity self = (PipeBlockEntity) (Object) this;
        if (!AdAstraPipeNetworks.needsRebuild(self)) {
            return;
        }
        int rate = MachineConfig.pipeRefreshRate;
        if (rate > 0 && Math.floorMod(gameTime, rate) == 0) {
            return;
        }
        cointcore$sources().clear();
        cointcore$consumers().clear();
        ((Pipe) self).findNodes(level, pos);
        AdAstraPipeNetworks.markClean(self);
    }
}
