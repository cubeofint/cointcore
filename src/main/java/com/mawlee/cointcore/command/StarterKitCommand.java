package com.mawlee.cointcore.command;

import com.mawlee.cointcore.config.StarterKitConfig;
import com.mawlee.cointcore.kit.StarterKitSavedData;
import com.mawlee.cointcore.kit.StarterKitService;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public final class StarterKitCommand {
    private StarterKitCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> cointCoreBranch() {
        return Commands.literal("starter")
                .then(Commands.literal("status")
                        .requires(StarterKitCommand::canClaim)
                        .executes(StarterKitCommand::status))
                .then(Commands.literal("set_from_inv")
                        .requires(StarterKitCommand::canAdmin)
                        .executes(StarterKitCommand::setFromInv))
                .then(Commands.literal("sync_cooldown")
                        .requires(StarterKitCommand::canAdmin)
                        .executes(StarterKitCommand::syncCooldown))
                .then(Commands.literal("reset_firstjoin")
                        .requires(StarterKitCommand::canAdmin)
                        .then(Commands.argument("players", GameProfileArgument.gameProfile())
                                .executes(StarterKitCommand::resetFirstJoin)));
    }

    private static boolean canClaim(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.STARTER_KIT);
        }
        return source.hasPermission(2);
    }

    private static boolean canAdmin(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.STARTER_KIT_ADMIN);
        }
        return source.hasPermission(2);
    }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String summary = StarterKitService.statusSummary(player);
        source.sendSuccess(
                () -> CointCoreMessages.forPlayer(player, CointCoreMessages.STARTER_KIT_STATUS, summary),
                false
        );
        return 1;
    }

    private static int setFromInv(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer admin = source.getPlayerOrException();
        StarterKitService.SetFromInvResult result = StarterKitService.setFromInventory(admin);
        if (result.success()) {
            source.sendSuccess(
                    () -> CointCoreMessages.forPlayer(admin, result.messageKey(), result.args()),
                    true
            );
            return 1;
        }
        if (result.rawMessage() != null) {
            source.sendFailure(Component.literal(result.rawMessage()));
        } else {
            source.sendFailure(CointCoreMessages.forPlayer(admin, result.messageKey(), result.args()));
        }
        return 0;
    }

    private static int syncCooldown(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!StarterKitService.syncCooldownFromConfig()) {
            if (source.getEntity() instanceof ServerPlayer player) {
                source.sendFailure(CointCoreMessages.forPlayer(
                        player,
                        CointCoreMessages.STARTER_KIT_MISSING,
                        StarterKitConfig.get().kitName()
                ));
            } else {
                source.sendFailure(CointCoreMessages.forConsole(
                        CointCoreMessages.STARTER_KIT_MISSING,
                        StarterKitConfig.get().kitName()
                ));
            }
            return 0;
        }
        long cooldown = StarterKitConfig.get().cooldownSeconds();
        String kitName = StarterKitConfig.get().kitName();
        if (source.getEntity() instanceof ServerPlayer player) {
            source.sendSuccess(
                    () -> CointCoreMessages.forPlayer(player, CointCoreMessages.STARTER_KIT_COOLDOWN_SYNCED, kitName, cooldown),
                    true
            );
        } else {
            source.sendSuccess(
                    () -> CointCoreMessages.forConsole(CointCoreMessages.STARTER_KIT_COOLDOWN_SYNCED, kitName, cooldown),
                    true
            );
        }
        return 1;
    }

    private static int resetFirstJoin(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "players");
        StarterKitSavedData data = StarterKitSavedData.get(source.getServer());
        int cleared = 0;
        for (GameProfile profile : profiles) {
            if (data.clearFirstJoin(profile.getId())) {
                cleared++;
            }
        }
        int finalCleared = cleared;
        if (source.getEntity() instanceof ServerPlayer player) {
            source.sendSuccess(
                    () -> CointCoreMessages.forPlayer(player, CointCoreMessages.STARTER_KIT_FIRSTJOIN_RESET, finalCleared),
                    true
            );
        } else {
            source.sendSuccess(
                    () -> CointCoreMessages.forConsole(CointCoreMessages.STARTER_KIT_FIRSTJOIN_RESET, finalCleared),
                    true
            );
        }
        return cleared;
    }
}
