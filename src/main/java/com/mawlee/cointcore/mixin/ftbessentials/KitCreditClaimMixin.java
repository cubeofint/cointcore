package com.mawlee.cointcore.mixin.ftbessentials;

import com.mawlee.cointcore.kit.KitCreditService;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbessentials.commands.impl.kit.KitCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Optional;

@Mixin(value = KitCommand.class, remap = false)
public abstract class KitCreditClaimMixin {
    @Inject(method = "giveKit", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cointcore$claimWithCredits(
            CommandSourceStack source,
            String name,
            Collection<ServerPlayer> players,
            CallbackInfoReturnable<Integer> cir
    ) throws CommandSyntaxException {
        Optional<Integer> remaining = KitCreditService.tryHandleSelfClaim(source, name, players);
        if (remaining.isEmpty()) {
            return;
        }

        ServerPlayer player = source.getPlayerOrException();
        int left = remaining.get();
        source.sendSuccess(
                () -> CointCoreMessages.forPlayer(player, CointCoreMessages.KIT_CREDIT_CLAIMED, name, left),
                true
        );
        cir.setReturnValue(1);
    }
}
