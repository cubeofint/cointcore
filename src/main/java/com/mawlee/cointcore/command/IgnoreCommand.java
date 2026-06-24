package com.mawlee.cointcore.command;

import com.mawlee.cointcore.ignore.IgnoreService;
import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class IgnoreCommand {
    private IgnoreCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("ignore")
                        .requires(IgnoreCommand::canUse)
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        suggestTargets(context.getSource()),
                                        builder
                                ))
                                .executes(IgnoreCommand::toggle))
        );
    }

    private static boolean canUse(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player && PermissionService.has(player, CointPermissionNodes.IGNORE);
    }

    private static int toggle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String targetName = StringArgumentType.getString(context, "target");

        Optional<UUID> targetId = IgnoreService.resolveTargetId(player.server, targetName);
        if (targetId.isEmpty()) {
            player.sendSystemMessage(CointCoreMessages.forPlayer(player, CointCoreMessages.IGNORE_PLAYER_NOT_FOUND, targetName));
            return 0;
        }

        IgnoreService.toggle(player, targetId.get(), targetName);
        return 1;
    }

    private static List<String> suggestTargets(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer viewer)) {
            return List.of();
        }

        List<String> names = new ArrayList<>();
        for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
            if (online.getUUID().equals(viewer.getUUID())) {
                continue;
            }

            names.add(online.getGameProfile().getName());
        }

        return names;
    }
}
