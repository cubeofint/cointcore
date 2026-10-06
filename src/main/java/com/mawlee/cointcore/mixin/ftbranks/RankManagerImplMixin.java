package com.mawlee.cointcore.mixin.ftbranks;

import com.mawlee.cointcore.ftbranks.FtbRanksLuckPermsBridge;
import dev.ftb.mods.ftbranks.api.PermissionValue;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = dev.ftb.mods.ftbranks.impl.RankManagerImpl.class, remap = false)
public abstract class RankManagerImplMixin {
    @Inject(
            method = "getPermissionValue(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;)Ldev/ftb/mods/ftbranks/api/PermissionValue;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void cointcore$luckPermsFallback(
            ServerPlayer player,
            String node,
            CallbackInfoReturnable<PermissionValue> cir
    ) {
        if (player == null || node == null || node.isEmpty()) {
            return;
        }

        PermissionValue original = cir.getReturnValue();
        PermissionValue bridged = FtbRanksLuckPermsBridge.afterFtbLookup(player, node, original);
        if (bridged != original) {
            cir.setReturnValue(bridged);
        }
    }
}
