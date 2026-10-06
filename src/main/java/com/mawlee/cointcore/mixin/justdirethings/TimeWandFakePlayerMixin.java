package com.mawlee.cointcore.mixin.justdirethings;

import com.direwolf20.justdirethings.common.items.TimeWand;
import com.mawlee.cointcore.tickaccel.TickAccelerationDeny;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Time Wand gates: FakePlayer + deny list on {@code spawnEntity}
 * (covers blocks that miss {@code MiscTools.isValidTickAccelBlock} heuristics).
 */
@Mixin(value = TimeWand.class, remap = false)
public abstract class TimeWandFakePlayerMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void cointcore$blockFakePlayer(
            Level level,
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
    ) {
        if (TickAccelerationDeny.blockFakePlayers() && player instanceof FakePlayer) {
            cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
        }
    }

    @Inject(method = "spawnEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$denySpawnOnDeniedBlock(
            Level level,
            Player player,
            BlockPos blockPos,
            ItemStack itemStack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (level.isClientSide || blockPos == null) {
            return;
        }
        if (TickAccelerationDeny.isDenied(level.getBlockState(blockPos))) {
            cir.setReturnValue(false);
        }
    }
}
