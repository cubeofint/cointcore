package com.mawlee.cointcore.command;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mawlee.cointcore.teleport.TplService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class TplCommand {
    private TplCommand() {
    }

    public static void apply(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandRegistration.replaceRootLiteral(dispatcher, "tpl", tplCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tplCommand() {
        return Commands.literal("tpl")
                .requires(TplCommand::canTpl)
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                suggestPlayerNames(context.getSource()),
                                builder
                        ))
                        .executes(TplCommand::teleportToPlayer));
    }

    private static boolean canTpl(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return PermissionService.has(player, CointPermissionNodes.TPL);
        }

        return source.hasPermission(2);
    }

    private static int teleportToPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String targetName = StringArgumentType.getString(context, "target");

        Optional<UUID> targetId = TplService.resolveTargetId(source.getServer(), targetName);
        if (targetId.isEmpty()) {
            sendFailure(source, CointCoreMessages.TPL_PLAYER_NOT_FOUND, targetName);
            return 0;
        }

        String resolvedName = TplService.resolveTargetName(source.getServer(), targetId.get()).orElse(targetName);
        return TplService.teleportToPlayer(source, targetId.get(), resolvedName) ? 1 : 0;
    }

    private static List<String> suggestPlayerNames(CommandSourceStack source) {
        List<String> names = new ArrayList<>();
        for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
            names.add(online.getGameProfile().getName());
        }
        return names;
    }

    private static void sendFailure(CommandSourceStack source, String key, Object... args) {
        source.sendFailure(messageFor(source, key, args));
    }

    public static Component messageFor(CommandSourceStack source, String key, Object... args) {
        return CointCoreMessages.forSource(source, key, args);
    }
}
