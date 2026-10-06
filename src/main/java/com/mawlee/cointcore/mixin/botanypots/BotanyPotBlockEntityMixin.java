package com.mawlee.cointcore.mixin.botanypots;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mawlee.cointcore.config.BotanyPotsPerfConfig;
import net.darkhax.bookshelf.common.api.util.IGameplayHelper;
import net.darkhax.bookshelf.common.api.util.TickAccumulator;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops hopper Botany Pots from calling {@code inventoryInsert} every tick into a full inventory.
 * Enforces a minimum export interval and a longer backoff when inserts make no progress.
 */
@Mixin(value = BotanyPotBlockEntity.class, remap = false)
public abstract class BotanyPotBlockEntityMixin {
    @Unique
    private static final ThreadLocal<Boolean> COINTCORE$EXPORT_ATTEMPTED = ThreadLocal.withInitial(() -> false);

    @Unique
    private static final ThreadLocal<Boolean> COINTCORE$EXPORT_MADE_PROGRESS = ThreadLocal.withInitial(() -> false);

    @Inject(method = "tickPot", at = @At("HEAD"), remap = false)
    private static void cointcore$resetExportTracking(
            Level level,
            BlockPos pos,
            BlockState state,
            BotanyPotBlockEntity pot,
            CallbackInfo ci
    ) {
        COINTCORE$EXPORT_ATTEMPTED.set(false);
        COINTCORE$EXPORT_MADE_PROGRESS.set(false);
    }

    @WrapOperation(
            method = "tickPot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/darkhax/bookshelf/common/api/util/IGameplayHelper;inventoryInsert(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
                    remap = false
            ),
            remap = false
    )
    private static ItemStack cointcore$trackExportProgress(
            IGameplayHelper helper,
            ServerLevel level,
            BlockPos pos,
            Direction side,
            ItemStack stack,
            Operation<ItemStack> original
    ) {
        if (!BotanyPotsPerfConfig.isEnabled()) {
            return original.call(helper, level, pos, side, stack);
        }

        int before = stack.getCount();
        ItemStack remainder = original.call(helper, level, pos, side, stack);
        COINTCORE$EXPORT_ATTEMPTED.set(true);
        if (remainder.isEmpty() || remainder.getCount() < before) {
            COINTCORE$EXPORT_MADE_PROGRESS.set(true);
        }
        return remainder;
    }

    /**
     * Replaces {@code exportCooldown.reset()} (ordinal 1 in {@code tickPot}; ordinal 0 is growthTime).
     */
    @WrapOperation(
            method = "tickPot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/darkhax/bookshelf/common/api/util/TickAccumulator;reset()V",
                    ordinal = 1,
                    remap = false
            ),
            remap = false
    )
    private static void cointcore$throttleExportCooldown(TickAccumulator cooldown, Operation<Void> original) {
        if (!BotanyPotsPerfConfig.isEnabled()) {
            original.call(cooldown);
            return;
        }

        boolean attempted = Boolean.TRUE.equals(COINTCORE$EXPORT_ATTEMPTED.get());
        boolean madeProgress = Boolean.TRUE.equals(COINTCORE$EXPORT_MADE_PROGRESS.get());
        COINTCORE$EXPORT_ATTEMPTED.set(false);
        COINTCORE$EXPORT_MADE_PROGRESS.set(false);

        int delay = attempted && !madeProgress
                ? BotanyPotsPerfConfig.getFullInventoryBackoffTicks()
                : BotanyPotsPerfConfig.getMinExportIntervalTicks();
        cooldown.setTicks(delay);
    }
}
