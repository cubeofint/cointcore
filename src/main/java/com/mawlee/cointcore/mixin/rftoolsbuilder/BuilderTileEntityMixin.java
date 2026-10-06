package com.mawlee.cointcore.mixin.rftoolsbuilder;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.config.MachinePerfConfig;
import com.mawlee.cointcore.rftools.RfToolsBuilderBudget;
import mcjty.lib.varia.LevelTools;
import mcjty.rftoolsbuilder.modules.builder.BuilderConfiguration;
import mcjty.rftoolsbuilder.modules.builder.blocks.BuilderTileEntity;
import mcjty.rftoolsbuilder.modules.builder.data.BuilderData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Caps RFTools Builder throughput:
 * <ul>
 *   <li>shape-card quarry: {@code base + infusion×factor} + staggered tick interval</li>
 *   <li>space chamber: hardcoded {@code 2 + infusion×40} remapped to the same base/infusion caps</li>
 *   <li>global per-tick block budget shared by all Builders</li>
 *   <li>one shape-formula rebuild and one new chunk ticket per server tick</li>
 *   <li>collect mode scans the chamber on a longer timer</li>
 *   <li>forced don't-wait mode</li>
 * </ul>
 */
@Mixin(value = BuilderTileEntity.class, remap = false)
public abstract class BuilderTileEntityMixin {
    @Shadow
    private Map<BlockPos, BlockState> cachedBlocks;

    @Shadow
    private ChunkPos cachedChunk;

    @Shadow
    private ChunkPos forcedChunk;

    @Shadow
    private int collectCounter;

    @Unique
    private boolean cointcore$deferShapeAdvance;

    @Unique
    private boolean cointcore$collectPhased;

    @Inject(method = "isWaitMode", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$forceDontWait(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "checkStateServerShaped", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$throttleShapedQuarry(CallbackInfo ci) {
        if (!MachinePerfConfig.isRfToolsBuilderEnabled()) {
            return;
        }

        if (cointcore$isOffTick()) {
            ci.cancel();
        }
    }

    @Inject(method = "tickServer", at = @At("HEAD"), remap = false)
    private void cointcore$phaseCollect(CallbackInfo ci) {
        if (cointcore$collectPhased || !MachinePerfConfig.isRfToolsBuilderEnabled()) {
            return;
        }
        cointcore$collectPhased = true;
        if (collectCounter != 0) {
            return;
        }
        int timer = MachinePerfConfig.getRfToolsMinCollectTimer();
        int phase = (int) Math.floorMod(((BlockEntity) (Object) this).getBlockPos().asLong(), timer);
        collectCounter = phase;
    }

    @Redirect(
            method = "collectItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/common/ModConfigSpec$IntValue;get()Ljava/lang/Object;",
                    remap = false
            ),
            remap = false
    )
    private Object cointcore$raiseCollectTimer(ModConfigSpec.IntValue value) {
        Object raw = value.get();
        if (!MachinePerfConfig.isRfToolsBuilderEnabled()
                || value != BuilderConfiguration.collectTimer
                || !(raw instanceof Integer amount)) {
            return raw;
        }
        return Math.max(amount, MachinePerfConfig.getRfToolsMinCollectTimer());
    }

    @Redirect(
            method = "checkStateServerShaped",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/common/ModConfigSpec$IntValue;get()Ljava/lang/Object;",
                    remap = false
            ),
            remap = false
    )
    private Object cointcore$capShapedSpeed(ModConfigSpec.IntValue value) {
        Object raw = value.get();
        if (!MachinePerfConfig.isRfToolsBuilderEnabled() || !(raw instanceof Integer amount)) {
            return raw;
        }

        if (value == BuilderConfiguration.quarryBaseSpeed) {
            return Math.min(amount, MachinePerfConfig.getRfToolsMaxQuarryBaseSpeed());
        }
        if (value == BuilderConfiguration.quarryInfusionSpeedFactor) {
            return Math.min(amount, MachinePerfConfig.getRfToolsMaxInfusionSpeedFactor());
        }
        return raw;
    }

    @WrapOperation(
            method = "checkStateServerShaped",
            at = @At(
                    value = "INVOKE",
                    target = "Lmcjty/rftoolsbuilder/modules/builder/blocks/BuilderTileEntity;handleBlockShaped(Lmcjty/rftoolsbuilder/modules/builder/data/BuilderData;)Lmcjty/rftoolsbuilder/modules/builder/data/BuilderData;"
            )
    )
    private BuilderData cointcore$budgetShapedOp(
            BuilderTileEntity instance,
            BuilderData data,
            Operation<BuilderData> original
    ) {
        cointcore$deferShapeAdvance = false;
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (!RfToolsBuilderBudget.tryConsume(level)) {
            return data;
        }
        BuilderData result = original.call(instance, data);
        if (cointcore$deferShapeAdvance) {
            RfToolsBuilderBudget.refundBlock(level);
        }
        return result;
    }

    @WrapOperation(
            method = "handleBlockShaped",
            at = @At(
                    value = "INVOKE",
                    target = "Lmcjty/rftoolsbuilder/modules/builder/blocks/BuilderTileEntity;getCachedBlocks(Lnet/minecraft/world/level/ChunkPos;)Ljava/util/Map;"
            )
    )
    private Map<BlockPos, BlockState> cointcore$deferColdChunkCache(
            BuilderTileEntity instance,
            ChunkPos chunk,
            Operation<Map<BlockPos, BlockState>> original
    ) {
        if (!MachinePerfConfig.isRfToolsBuilderEnabled()) {
            return original.call(instance, chunk);
        }
        if (cointcore$deferShapeAdvance) {
            return Map.of();
        }
        if (cointcore$cacheNeedsRebuild(chunk)) {
            Level level = ((BlockEntity) (Object) this).getLevel();
            if (!RfToolsBuilderBudget.tryConsumeFormulaRebuild(level)) {
                cointcore$deferShapeAdvance = true;
                return Map.of();
            }
        }
        return original.call(instance, chunk);
    }

    @WrapOperation(
            method = "handleBlockShaped",
            at = @At(
                    value = "INVOKE",
                    target = "Lmcjty/rftoolsbuilder/modules/builder/blocks/BuilderTileEntity;nextLocation(Lmcjty/rftoolsbuilder/modules/builder/data/BuilderData;)Lmcjty/rftoolsbuilder/modules/builder/data/BuilderData;"
            )
    )
    private BuilderData cointcore$holdScanUntilFormula(
            BuilderTileEntity instance,
            BuilderData data,
            Operation<BuilderData> original
    ) {
        if (cointcore$deferShapeAdvance) {
            return data;
        }
        return original.call(instance, data);
    }

    @WrapOperation(
            method = "handleSingleBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lmcjty/rftoolsbuilder/modules/builder/blocks/BuilderTileEntity;chunkLoad(II)Z"
            )
    )
    private boolean cointcore$budgetChunkLoad(
            BuilderTileEntity instance,
            int x,
            int z,
            Operation<Boolean> original
    ) {
        if (!MachinePerfConfig.isRfToolsBuilderEnabled()) {
            return original.call(instance, x, z);
        }
        Level level = ((BlockEntity) (Object) this).getLevel();
        ChunkPos target = new ChunkPos(x >> 4, z >> 4);
        boolean alreadyTicketed = target.equals(forcedChunk);
        boolean loaded = level != null && LevelTools.isLoaded(level, new BlockPos(x, 0, z));
        if (loaded || alreadyTicketed) {
            return original.call(instance, x, z);
        }
        if (!RfToolsBuilderBudget.tryConsumeChunkLoad(level)) {
            RfToolsBuilderBudget.refundBlock(level);
            return false;
        }
        return original.call(instance, x, z);
    }

    /**
     * Space chamber loop: {@code while (i < 2 + factor * 40)}. Remap to quarry base/infusion caps
     * and skip entire chamber work on shaped-interval off-ticks.
     */
    @ModifyConstant(method = "tickServer", constant = @Constant(floatValue = 2.0F), remap = false)
    private float cointcore$capChamberBase(float original) {
        if (!MachinePerfConfig.isRfToolsBuilderEnabled()) {
            return original;
        }
        if (cointcore$isOffTick()) {
            return 0.0F;
        }
        return Math.min(original, (float) MachinePerfConfig.getRfToolsMaxQuarryBaseSpeed());
    }

    @ModifyConstant(method = "tickServer", constant = @Constant(floatValue = 40.0F), remap = false)
    private float cointcore$capChamberInfusion(float original) {
        if (!MachinePerfConfig.isRfToolsBuilderEnabled()) {
            return original;
        }
        if (cointcore$isOffTick()) {
            return 0.0F;
        }
        return Math.min(original, (float) MachinePerfConfig.getRfToolsMaxInfusionSpeedFactor());
    }

    @WrapOperation(
            method = "tickServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lmcjty/rftoolsbuilder/modules/builder/blocks/BuilderTileEntity;handleBlock(Lmcjty/rftoolsbuilder/modules/builder/data/BuilderData;Lnet/minecraft/world/level/Level;)Lmcjty/rftoolsbuilder/modules/builder/data/BuilderData;"
            )
    )
    private BuilderData cointcore$budgetChamberOp(
            BuilderTileEntity instance,
            BuilderData data,
            Level world,
            Operation<BuilderData> original
    ) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (!RfToolsBuilderBudget.tryConsume(level)) {
            return data;
        }
        return original.call(instance, data, world);
    }

    @Unique
    private boolean cointcore$isOffTick() {
        if (!MachinePerfConfig.isRfToolsBuilderEnabled()) {
            return false;
        }
        int interval = MachinePerfConfig.getRfToolsShapedTickInterval();
        if (interval <= 1) {
            return false;
        }
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level == null) {
            return true;
        }
        long phase = Math.floorMod(((BlockEntity) (Object) this).getBlockPos().asLong(), interval);
        return Math.floorMod(level.getGameTime(), interval) != phase;
    }

    @Unique
    private boolean cointcore$cacheNeedsRebuild(ChunkPos chunk) {
        return cachedBlocks == null || cachedChunk == null || !cachedChunk.equals(chunk);
    }
}
