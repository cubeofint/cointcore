package com.mawlee.cointcore.mixin.draconicevolution;

import com.brandon3055.brandonscore.blocks.EntityBlockBCore;
import com.brandon3055.brandonscore.blocks.TileBCore;
import com.brandon3055.draconicevolution.blocks.PlacedItem;
import com.brandon3055.draconicevolution.init.DEContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Placed Item enables BrandonsCore ticking only so {@link TileBCore#tick()} can poll the
 * inventory for changes. Player insert, extract and rotate already call {@code tick()}
 * immediately. The server idle poll is staggered so a static stack is not serialized every tick.
 * The client ticker stays vanilla.
 */
@Mixin(value = EntityBlockBCore.class, remap = false)
public abstract class PlacedItemTickerMixin {
    private static final int PLACED_ITEM_SYNC_INTERVAL = 20;

    @Inject(method = "getTicker", at = @At("HEAD"), cancellable = true, remap = false)
    private <T extends BlockEntity> void cointcore$throttlePlacedItemTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type,
            CallbackInfoReturnable<BlockEntityTicker<T>> cir
    ) {
        if (!((Object) this instanceof PlacedItem) || type != DEContent.TILE_PLACED_ITEM.get()) {
            return;
        }
        if (level == null || level.isClientSide()) {
            return;
        }

        cir.setReturnValue((Level world, BlockPos pos, BlockState blockState, T blockEntity) -> {
            if (world == null || !(blockEntity instanceof TileBCore tile)) {
                return;
            }
            long phase = Math.floorMod(pos.asLong(), PLACED_ITEM_SYNC_INTERVAL);
            if (Math.floorMod(world.getGameTime(), PLACED_ITEM_SYNC_INTERVAL) != phase) {
                return;
            }
            tile.tick();
        });
    }
}
