package com.mawlee.cointcore.mixin.sfm;

import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import com.mawlee.cointcore.config.SfmPerfConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import ca.teamdman.sfm.common.registry.registration.SFMDataComponents;
import com.mawlee.cointcore.sfm.BoundBlockLimit;
import com.mawlee.cointcore.sfm.LimitedManager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Enforces the bound-block limit and soft-throttles execution at high MSPT.
 * Injects before {@code Program.tick}; the manager tick counter has already advanced.
 * <p>
 * The MSPT throttle still runs 1 of every N ticks; oversized bindings pause until corrected.
 */
@Mixin(value = ManagerBlockEntity.class, remap = false)
public abstract class ManagerBlockEntityMixin implements LimitedManager {
    @Unique private final BoundBlockLimit.Cache cointcore$blockLimitCache = new BoundBlockLimit.Cache();
    @Unique private boolean cointcore$reportedBlockLimit;

    @Override
    public boolean cointcore$exceedsBoundBlockLimit() {
        int limit = SfmPerfConfig.getMaxBoundBlocksPerManager();
        if (limit <= 0) {
            cointcore$reportedBlockLimit = false;
            return false;
        }
        var manager = (ManagerBlockEntity) (Object) this;
        var disk = manager.getDisk();
        var labels = disk == null ? null : disk.get(SFMDataComponents.LABEL_POSITION_HOLDER.get());
        boolean exceeded = labels != null && cointcore$blockLimitCache.exceeds(labels, limit, () -> labels.labels().values());
        if (exceeded && !cointcore$reportedBlockLimit) {
            com.mojang.logging.LogUtils.getLogger().warn("SFM manager at {} in {} paused: more than {} bound blocks",
                    manager.getBlockPos(), manager.getLevel().dimension().location(), limit);
        }
        cointcore$reportedBlockLimit = exceeded;
        return exceeded;
    }

    @Inject(method = "createMenu", at = @At("HEAD"), require = 1)
    private void cointcore$explainPausedManager(int id, Inventory inventory, CallbackInfoReturnable<AbstractContainerMenu> callback) {
        if (cointcore$exceedsBoundBlockLimit()) inventory.player.displayClientMessage(
                Component.translatable("message.cointcore.sfm.block_limit_paused", SfmPerfConfig.getMaxBoundBlocksPerManager()), false);
    }
    @Inject(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lca/teamdman/sfml/ast/Program;tick(Lca/teamdman/sfm/common/blockentity/ManagerBlockEntity;)Z",
                    remap = false
            ),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void cointcore$skipProgramTickWhenMsptHigh(
            Level level,
            BlockPos pos,
            BlockState state,
            ManagerBlockEntity manager,
            CallbackInfo ci
    ) {
        if (((LimitedManager) manager).cointcore$exceedsBoundBlockLimit()
                || SfmPerfConfig.shouldSkipProgramTick(manager.getTick())) {
            ci.cancel();
        }
    }
}
