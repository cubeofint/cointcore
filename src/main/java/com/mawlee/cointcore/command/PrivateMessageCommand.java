package com.mawlee.cointcore.command;

import com.mawlee.cointcore.lang.CointCoreMessages;
import com.mawlee.cointcore.message.PrivateMessageService;
import com.mawlee.cointcore.permission.CointPermissionNodes;
import com.mawlee.cointcore.permission.PermissionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class PrivateMessageCommand {
    private static final List<String> MESSAGE_ALIASES = List.of("m", "msg", "w", "tell", "whisper");

    private PrivateMessageCommand() {
    }

    public static void applyMessageCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String alias : MESSAGE_ALIASES) {
            CommandRegistration.replaceRootLiteral(dispatcher, alias, messageCommand(alias));
        }
    }

    public static void applyReplyCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandRegistration.unregisterRootLiteral(dispatcher, "r");
        dispatcher.register(Commands.literal("r")
                .requires(PrivateMessageCommand::canUse)
                .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(PrivateMessageCommand::replyMessage)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> messageCommand(String name) {
        return Commands.literal(name)
                .requires(PrivateMessageCommand::canUse)
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                suggestTargets(context.getSource()),
                                builder
                        ))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(PrivateMessageCommand::sendMessage)));
    }

    private static boolean canUse(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player && PermissionService.has(player, CointPermissionNodes.MESSAGE);
    }

    private static int sendMessage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        String targetName = StringArgumentType.getString(context, "target");
        String text = StringArgumentType.getString(context, "text");

        if (text.isBlank()) {
            return 0;
        }

        ServerPlayer target = PrivateMessageService.findReachableTarget(sender.server, sender, targetName);
        if (target == null) {
            sender.sendSystemMessage(CointCoreMessages.forPlayer(
                    sender,
                    CointCoreMessages.MSG_PLAYER_NOT_FOUND,
                    targetName
            ));
            return 0;
        }

        PrivateMessageService.send(sender, target, text);
        return 1;
    }

    private static int replyMessage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        String text = StringArgumentType.getString(context, "text");

        if (text.isBlank()) {
            return 0;
        }

        PrivateMessageService.reply(sender, text);
        return 1;
    }

    private static List<String> suggestTargets(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer sender)) {
            return List.of();
        }

        List<String> names = new ArrayList<>();
        for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
            if (online.getUUID().equals(sender.getUUID())) {
                continue;
            }

            if (PrivateMessageService.isReachable(online)) {
                names.add(online.getGameProfile().getName());
            }
        }

        return names;
    }
}
