package com.mawlee.cointcore.mixin.ftbessentials;

import com.mawlee.cointcore.config.StarterKitConfig;
import com.mawlee.cointcore.kit.KitCreditService;
import com.mawlee.cointcore.kit.StarterKitService;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbessentials.commands.impl.kit.KitCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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
        if (tryHandleStarterClaim(source, name, players, cir)) {
            return;
        }

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

    private static boolean tryHandleStarterClaim(
            CommandSourceStack source,
            String name,
            Collection<ServerPlayer> players,
            CallbackInfoReturnable<Integer> cir
    ) throws CommandSyntaxException {
        if (!StarterKitService.isAvailable()) {
            return false;
        }
        StarterKitConfig.Settings settings = StarterKitConfig.get();
        if (!settings.enabled() || name == null || !settings.kitName().equalsIgnoreCase(name.trim())) {
            return false;
        }
        if (players.size() != 1 || !(source.getEntity() instanceof ServerPlayer player) || !players.contains(player)) {
            return false;
        }
        // Admin /ftb_kit give stays on vanilla FTB path.
        if (source.hasPermission(Commands.LEVEL_GAMEMASTERS)) {
            return false;
        }
        if (!PermissionService.has(player, CointPermissionNodes.STARTER_KIT)) {
            source.sendFailure(CointCoreMessages.forPlayer(player, CointCoreMessages.STARTER_KIT_DISABLED));
            cir.setReturnValue(0);
            return true;
        }

        StarterKitService.ClaimResult result = StarterKitService.claim(player);
        if (result.success()) {
            source.sendSuccess(
                    () -> CointCoreMessages.forPlayer(player, result.messageKey(), result.args()),
                    false
            );
            cir.setReturnValue(1);
            return true;
        }
        if (result.rawMessage() != null) {
            source.sendFailure(Component.literal(result.rawMessage()));
        } else {
            source.sendFailure(CointCoreMessages.forPlayer(player, result.messageKey(), result.args()));
        }
        cir.setReturnValue(0);
        return true;
    }
}
