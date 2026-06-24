package com.mawlee.cointcore.mixin;

import com.mawlee.cointcore.vanish.VanishInteractionTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;
import java.util.function.Consumer;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "openMenu(Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)Ljava/util/OptionalInt;", at = @At("HEAD"))
    private void cointcore$trackContainerOpen(
            MenuProvider menuProvider,
            Consumer<Component> displayNameGenerator,
            CallbackInfoReturnable<OptionalInt> cir
    ) {
        if (menuProvider instanceof BlockEntity blockEntity) {
            VanishInteractionTracker.updateBlockInteraction((ServerPlayer) (Object) this, blockEntity.getBlockPos());
        }
    }
}
