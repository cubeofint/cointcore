package com.mawlee.cointcore.mixin.modularrouters;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mawlee.cointcore.modularrouters.GuardedPlayerItemHandler;
import com.mawlee.cointcore.modularrouters.PlayerInventoryMenuGuard;
import me.desht.modularrouters.block.tile.ModularRouterBlockEntity;
import me.desht.modularrouters.logic.compiled.CompiledPlayerModule;
import me.desht.modularrouters.logic.settings.TransferDirection;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Prevents Player Module from yanking items out of a player who currently has a GUI open
 * (wireless terminal / portable cell / backpack, etc.) — classic open-GUI dupe vector.
 */
@Mixin(value = CompiledPlayerModule.class, remap = false)
public abstract class CompiledPlayerModuleMixin {
    @Shadow
    public abstract TransferDirection getTransferDirection();

    @Invoker("getPlayer")
    abstract Player cointcore$invokeGetPlayer();

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private void cointcore$blockExtractWhileGuiOpen(
            ModularRouterBlockEntity router,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (getTransferDirection() != TransferDirection.TO_ROUTER) {
            return;
        }

        if (PlayerInventoryMenuGuard.blocksExtraction(cointcore$invokeGetPlayer())) {
            cir.setReturnValue(false);
        }
    }

    @WrapMethod(method = "getHandler", remap = false)
    private IItemHandler cointcore$guardExtracts(Player player, Operation<IItemHandler> original) {
        IItemHandler handler = original.call(player);
        if (handler == null) {
            return null;
        }
        return new GuardedPlayerItemHandler(player, handler);
    }
}
