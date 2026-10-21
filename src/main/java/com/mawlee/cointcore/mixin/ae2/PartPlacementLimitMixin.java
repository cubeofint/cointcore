package com.mawlee.cointcore.mixin.ae2;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartItem;
import appeng.parts.PartPlacement;
import com.mawlee.cointcore.chunklimit.ChunkLimitService;
import com.mawlee.cointcore.config.ChunkLimitConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chunk / team limits for AE2 cable-bus parts (storage buses, …) from {@code chunk-limits.json}. A refused
 * {@code placePart} returns null, which AE2 turns into {@code InteractionResult.FAIL} without using the item.
 *
 * <p>Verified against AE2 19.2.18.
 */
@Mixin(value = PartPlacement.class, remap = false)
public abstract class PartPlacementLimitMixin {
    @Inject(method = "placePart", at = @At("HEAD"), cancellable = true, remap = false)
    private static <T extends IPart> void cointcore$checkPartLimit(Player player, Level level, IPartItem<T> partItem,
                                                                  DataComponentMap components, BlockPos pos,
                                                                  Direction side, CallbackInfoReturnable<T> cir) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)
                || !ChunkLimitConfig.isEnabled()) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(partItem.asItem());
        if (ChunkLimitConfig.isLimitedPart(id) && !ChunkLimitService.allowPartPlacement(serverPlayer, serverLevel, pos, id)) {
            cir.setReturnValue(null);
        }
    }
}
